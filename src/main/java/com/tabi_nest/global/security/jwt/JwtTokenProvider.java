package com.tabi_nest.global.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Map;

/**
 * JWT 생성/검증 유틸.
 *
 * 사용처:
 * - AuthApiController: 로컬 로그인 성공 시 JWT 발급
 * - OAuth2LoginSuccessHandler: OAuth 로그인 성공 시 JWT 발급
 * - JwtAuthFilter: 요청마다 JWT를 검증해서 인증 정보를 SecurityContext에 올림
 */
@Component
public class JwtTokenProvider {

    private final JwtProperties props;
    private final SecretKey key;

    public JwtTokenProvider(JwtProperties props) {
        this.props = props;

        // secret이 너무 짧으면 서명 키로 사용 불가 (HS256 권장: 최소 32바이트)
        String secret = props.getSecret();
        if (secret == null || secret.isBlank() || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            // 개발 편의상 예외를 던져서 "키를 넣어라"를 명확히 함
            throw new IllegalStateException("JWT_SECRET is missing or too short. Please set JWT_SECRET (>= 32 bytes) in .env or environment variables.");
        }
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Access Token 발급
     * @param subject 보통 userId 또는 email 같은 고유값 (여기서는 userId를 문자열로)
     * @param claims  추가로 담을 값 (role, name 등)
     */
    public String createAccessToken(String subject, Map<String, Object> claims) {
        Instant now = Instant.now();
        Instant exp = now.plus(props.getAccessExpMinutes(), ChronoUnit.MINUTES);

        return Jwts.builder()
                .subject(subject)
                .issuedAt(Date.from(now))
                .expiration(Date.from(exp))
                .claims(claims)
                .signWith(key)
                .compact();
    }

    /** 서명/만료 검증 후 Claims 반환 */
    public Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean isValid(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
