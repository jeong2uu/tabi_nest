package com.tabi_nest.global.auth.controller;

import com.tabi_nest.global.auth.dto.LoginRequest;
import com.tabi_nest.global.auth.dto.SignupRequest;
import com.tabi_nest.global.auth.dto.UserSessionDto;
import com.tabi_nest.global.auth.service.AuthService;
import com.tabi_nest.global.security.jwt.JwtCookieUtil;
import com.tabi_nest.global.security.jwt.JwtTokenProvider;
import com.tabi_nest.user.domain.User;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * JWT 기반 로그인/회원가입 API
 *
 * 기존(HttpSession)과 달라진 점:
 * - 서버 세션에 사용자 정보를 저장하지 않는다.
 * - 로그인 성공 시 JWT(Access Token)를 발급하여
 *   1) 응답 JSON으로도 내려주고
 *   2) HttpOnly 쿠키로도 내려준다(프론트가 토큰을 저장하지 않아도 됨)
 *
 * 프론트(JS) 사용처:
 * - static/js/auth-modal.js 에서 /api/auth/login, /api/auth/signup 호출
 */
@RestController
@RequestMapping("/api/auth")
public class AuthApiController {

    private final AuthService authService;
    private final JwtTokenProvider jwtTokenProvider;
    private final JwtCookieUtil cookieUtil;

    public AuthApiController(AuthService authService, JwtTokenProvider jwtTokenProvider, JwtCookieUtil cookieUtil) {
        this.authService = authService;
        this.jwtTokenProvider = jwtTokenProvider;
        this.cookieUtil = cookieUtil;
    }

    @PostMapping("/signup")
    public ResponseEntity<?> signup(@RequestBody SignupRequest req, HttpServletResponse res) {
        try {
            User u = authService.signup(req);

            // JWT 발급 (subject=userId)
            String token = jwtTokenProvider.createAccessToken(
                    String.valueOf(u.getId()),
                    Map.of("role", u.getRole(), "name", u.getName(), "email", u.getEmail())
            );

            // HttpOnly 쿠키로 내려주기 (권장)
            cookieUtil.setAccessTokenCookie(res, token);

            // 응답 JSON (필요하면 프론트에서 Authorization 헤더 방식으로도 쓸 수 있게 token 포함)
            UserSessionDto dto = new UserSessionDto(u.getId(), u.getEmail(), u.getName(), u.getRole());
            return ResponseEntity.ok(Map.of(
                    "user", dto,
                    "accessToken", token
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest req, HttpServletResponse res) {
        try {
            User u = authService.login(req);

            String token = jwtTokenProvider.createAccessToken(
                    String.valueOf(u.getId()),
                    Map.of("role", u.getRole(), "name", u.getName(), "email", u.getEmail())
            );

            cookieUtil.setAccessTokenCookie(res, token);

            UserSessionDto dto = new UserSessionDto(u.getId(), u.getEmail(), u.getName(), u.getRole());
            return ResponseEntity.ok(Map.of(
                    "user", dto,
                    "accessToken", token
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletResponse res) {
        // 쿠키 삭제만 하면 됨 (서버 세션이 없으므로 invalidate 할 대상 없음)
        cookieUtil.clearAccessTokenCookie(res);
        return ResponseEntity.ok(Map.of("ok", true));
    }

    /**
     * 현재 로그인 사용자 조회
     *
     * 사용처:
     * - navbar에서 로그인 상태 표시(로그인 버튼/환영문구 등)
     *
     * 동작:
     * - JwtAuthFilter가 이미 SecurityContext에 인증을 올려둔 상태라고 가정
     * - principal에는 userId(subject)가 들어 있음
     */
    @GetMapping("/me")
    public ResponseEntity<?> me() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getPrincipal() == null || "anonymousUser".equals(auth.getPrincipal())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Not logged in"));
        }
        return ResponseEntity.ok(Map.of(
                "userId", auth.getPrincipal(),
                "role", auth.getAuthorities().stream().findFirst().map(Object::toString).orElse("ROLE_USER")
        ));
    }

    /**
     * 세션 체크용 ping → JWT에서는 토큰 유효성 체크로 대체
     * - 프론트에서 주기적으로 호출해서 401이면 "로그아웃 상태"로 UI 전환
     */
    @GetMapping("/ping")
    public ResponseEntity<?> ping() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getPrincipal() == null || "anonymousUser".equals(auth.getPrincipal())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("ok", false));
        }
        return ResponseEntity.ok(Map.of("ok", true));
    }
}
