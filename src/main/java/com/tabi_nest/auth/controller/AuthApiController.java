package com.tabi_nest.auth.controller;

import com.tabi_nest.auth.dto.LoginRequest;
import com.tabi_nest.auth.dto.SignupRequest;
import com.tabi_nest.auth.dto.UserSessionDto;
import com.tabi_nest.auth.service.AuthService;
import com.tabi_nest.user.domain.User;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthApiController {

    public static final String SESSION_KEY = "LOGIN_USER";

    private final AuthService authService;

    public AuthApiController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/signup")
    public ResponseEntity<?> signup(@RequestBody SignupRequest req, HttpSession session) {
        try {
            User u = authService.signup(req);
            UserSessionDto dto = new UserSessionDto(u.getId(), u.getEmail(), u.getName(), u.getRole());
            session.setAttribute(SESSION_KEY, dto);
            return ResponseEntity.ok(dto);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest req, HttpSession session) {
        try {
            User u = authService.login(req);
            UserSessionDto dto = new UserSessionDto(u.getId(), u.getEmail(), u.getName(), u.getRole());
            session.setAttribute(SESSION_KEY, dto);
            return ResponseEntity.ok(dto);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpSession session) {
        session.invalidate();
        return ResponseEntity.ok(Map.of("ok", true));
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(HttpSession session) {
        Object o = session.getAttribute(SESSION_KEY);
        if (o == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Not logged in"));
        }
        return ResponseEntity.ok(o);
    }

    /** lightweight endpoint for 30-min session check from JS */
    @GetMapping("/ping")
    public ResponseEntity<?> ping(HttpSession session) {
        Object o = session.getAttribute(SESSION_KEY);
        if (o == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("ok", false));
        }
        return ResponseEntity.ok(Map.of("ok", true));
    }
}
