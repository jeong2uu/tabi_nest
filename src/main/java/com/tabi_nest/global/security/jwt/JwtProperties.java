package com.tabi_nest.global.security.jwt;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * application.yaml의 jwt.* 설정을 바인딩하는 클래스.
 *
 * 사용처:
 * - JwtTokenProvider: 토큰 생성/검증 시 secret 및 만료시간 사용
 * - JwtCookieUtil: 쿠키 이름 사용
 */
@Data
@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    /**
     * JWT 서명에 사용되는 비밀키.
     * - 절대 Git에 커밋 금지
     * - .env 또는 서버 환경변수로 주입 추천
     */
    private String secret;
    private int accessExpMinutes = 30;  /* Access Token 만료(분) */
    private String cookieName = "TB_ACCESS";    /* HttpOnly 쿠키 이름 */

}
