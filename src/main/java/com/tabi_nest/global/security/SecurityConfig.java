package com.tabi_nest.global.security;

import com.tabi_nest.global.security.jwt.JwtAuthFilter;
import com.tabi_nest.global.security.jwt.JwtProperties;
import com.tabi_nest.global.security.jwt.JwtTokenProvider;
import com.tabi_nest.global.security.oauth.CustomOAuth2UserService;
import com.tabi_nest.global.security.oauth.OAuth2LoginSuccessHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security 설정
 *
 * 목표:
 * - 세션 기반(HttpSession) → JWT 기반으로 전환
 * - JWT는 HttpOnly 쿠키 또는 Authorization 헤더로 전달
 * - OAuth 로그인 성공 시에도 JWT를 발급해서 동일한 인증 방식으로 통일
 */
@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(
            HttpSecurity http,
            JwtTokenProvider jwtTokenProvider,
            JwtProperties jwtProperties,
            CustomOAuth2UserService oAuth2UserService,
            OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler
    ) throws Exception {

        // JWT 기반이면 서버 세션을 만들 필요가 없으므로 STATELESS 권장
        http.sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        // CSRF:
        // - JWT를 Authorization 헤더로만 보내면 CSRF 위험이 낮음
        // - 쿠키에 담는 경우에는 SameSite 설정/CSRF 토큰 전략을 고려해야 함
        // 여기서는 개발 편의상 disable (운영에서는 정책 결정 필요)
        http.csrf(csrf -> csrf.disable());

        http.authorizeHttpRequests(auth -> auth
                // ✅ 누구나 접근 가능
                .requestMatchers("/", "/home", "/login/**", "/signup").permitAll()
                // ✅ 정적 리소스 모두 공개 (중요!)
                .requestMatchers("/css/**", "/js/**", "/images/**", "/map/**", "/data/**", "/favicon.ico").permitAll()
                // ✅ OAuth 진입/콜백도 보통 permitAll
                .requestMatchers("/oauth2/**", "/api/i18n/**", "/api/regions/**", "/api/auth/**", "/login/oauth2/**")
                .permitAll()
                .anyRequest().authenticated()
        );

        // JWT 필터 등록 (UsernamePasswordAuthenticationFilter 보다 먼저)
        http.addFilterBefore(new JwtAuthFilter(jwtTokenProvider, jwtProperties), UsernamePasswordAuthenticationFilter.class);

        // OAuth2 로그인 활성화 (google/naver/kakao)
        http.oauth2Login(oauth -> oauth
                .userInfoEndpoint(user -> user.userService(oAuth2UserService))
                .successHandler(oAuth2LoginSuccessHandler)
        );

        // 기본 로그아웃 URL은 /logout (필요 시 커스텀)
        http.logout(logout -> logout.logoutSuccessUrl("/"));

        return http.build();
    }
}
