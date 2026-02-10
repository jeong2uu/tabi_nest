package com.tabi_nest.global.security.jwt;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * 매 요청마다 JWT를 읽어서 인증 컨텍스트(SecurityContext)를 세팅하는 필터.
 *
 * JWT를 어디서 읽나?
 * 1) Authorization 헤더: "Bearer <token>"
 * 2) HttpOnly 쿠키: jwt.cookie-name(TB_ACCESS)
 *
 * 사용처:
 * - SecurityConfig에서 filterChain에 등록
 */
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final JwtProperties props;

    public JwtAuthFilter(JwtTokenProvider jwtTokenProvider, JwtProperties props) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.props = props;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String token = resolveToken(request);

        if (token != null && jwtTokenProvider.isValid(token)) {
            Claims claims = jwtTokenProvider.parseClaims(token);

            String userId = claims.getSubject(); // createAccessToken(subject)에 넣은 값
            String role = (String) claims.get("role");

            // Spring Security에서는 권한이 "ROLE_" prefix 를 갖는 경우가 많음
            String authority = (role != null && role.startsWith("ROLE_")) ? role : "ROLE_" + (role == null ? "USER" : role);

            var auth = new UsernamePasswordAuthenticationToken(
                    userId, // principal
                    null,
                    List.of(new SimpleGrantedAuthority(authority))
            );

            // 인증 컨텍스트 등록
            SecurityContextHolder.getContext().setAuthentication(auth);
        }

        filterChain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
        // 1) Authorization: Bearer xxx
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }

        // 2) Cookie
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie c : cookies) {
                if (props.getCookieName().equals(c.getName())) {
                    return c.getValue();
                }
            }
        }
        return null;
    }
}
