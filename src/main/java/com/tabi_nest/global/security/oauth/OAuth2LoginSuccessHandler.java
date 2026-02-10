package com.tabi_nest.global.security.oauth;

import com.tabi_nest.global.security.jwt.JwtCookieUtil;
import com.tabi_nest.global.security.jwt.JwtTokenProvider;
import com.tabi_nest.user.domain.User;
import com.tabi_nest.user.repository.UserRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

/**
 * OAuth 로그인 성공 직후 JWT를 발급하여 HttpOnly 쿠키로 내려주는 핸들러.
 *
 * 사용처:
 * - SecurityConfig.oauth2Login().successHandler(...)
 *
 * 동작:
 * 1) OAuth2User attributes에서 providerId 추출
 * 2) users 테이블에서 provider/provider_id로 User 조회
 * 3) JWT 발급 후 쿠키 설정
 * 4) 홈으로 리다이렉트
 */
@Component
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final JwtCookieUtil cookieUtil;

    public OAuth2LoginSuccessHandler(UserRepository userRepository, JwtTokenProvider jwtTokenProvider, JwtCookieUtil cookieUtil) {
        this.userRepository = userRepository;
        this.jwtTokenProvider = jwtTokenProvider;
        this.cookieUtil = cookieUtil;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication)
            throws IOException, ServletException {

        if (authentication.getPrincipal() instanceof OAuth2User oAuth2User) {
            // registrationId는 request에서 꺼내기 어렵기 때문에 attributes를 보고 판단(간단히)
            Map<String, Object> attrs = oAuth2User.getAttributes();

            // Kakao는 id, Naver는 response.id, Google은 sub
            String provider = "L";
            String providerId = null;

            if (attrs.get("id") != null) {
                provider = "K";
                providerId = String.valueOf(attrs.get("id"));
            } else if (attrs.get("sub") != null) {
                provider = "G";
                providerId = String.valueOf(attrs.get("sub"));
            } else if (attrs.get("response") instanceof Map<?, ?> m && m.get("id") != null) {
                provider = "N";
                providerId = String.valueOf(m.get("id"));
            }

            if (providerId != null) {
                User user = userRepository.findByProviderAndProviderId(provider, providerId).orElse(null);
                if (user != null) {
                    String jwt = jwtTokenProvider.createAccessToken(
                            String.valueOf(user.getId()),
                            Map.of("role", user.getRole(), "name", user.getName(), "email", user.getEmail())
                    );
                    cookieUtil.setAccessTokenCookie(response, jwt);
                }
            }
        }

        response.sendRedirect("/");
    }
}
