package com.tabi_nest.global.security.jwt;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;

/**
 * JWT를 HttpOnly 쿠키로 내려주기 위한 유틸.
 *
 * 사용처:
 * - AuthApiController.login(): 로그인 성공 시 쿠키 세팅
 * - AuthApiController.logout(): 로그아웃 시 쿠키 삭제
 * - OAuth2LoginSuccessHandler: OAuth 성공 시 쿠키 세팅
 */
@Component
public class JwtCookieUtil {

    private final JwtProperties props;

    public JwtCookieUtil(JwtProperties props) {
        this.props = props;
    }

    /** HttpOnly 쿠키로 JWT 설정 */
    public void setAccessTokenCookie(HttpServletResponse res, String jwt) {
        Cookie c = new Cookie(props.getCookieName(), jwt);
        c.setHttpOnly(true);
        c.setSecure(false); // 운영(HTTPS)에서는 true 권장
        c.setPath("/");
        // 쿠키 만료는 Access Token 만료와 동일하게 맞추는 편이 직관적
        c.setMaxAge(props.getAccessExpMinutes() * 60);
        // SameSite는 Servlet Cookie API로 직접 설정이 어려워 header로 처리 가능 (필요 시 확장)
        res.addCookie(c);
    }

    /** 쿠키 삭제 */
    public void clearAccessTokenCookie(HttpServletResponse res) {
        Cookie c = new Cookie(props.getCookieName(), "");
        c.setPath("/");
        c.setMaxAge(0);
        res.addCookie(c);
    }
}
