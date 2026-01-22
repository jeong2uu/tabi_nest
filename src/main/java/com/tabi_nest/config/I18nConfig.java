package com.tabi_nest.config;

import java.time.Duration;
import java.util.Locale;

import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.i18n.CookieLocaleResolver;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;

@Configuration
public class I18nConfig implements WebMvcConfigurer {

    @Bean
    public MessageSource messageSource() {
        ReloadableResourceBundleMessageSource ms = new ReloadableResourceBundleMessageSource();
        ms.setBasename("classpath:messages");
        ms.setDefaultEncoding("UTF-8");
        ms.setFallbackToSystemLocale(false);
        return ms;
    }

    @Bean
    public LocaleResolver localeResolver() {
        // ✅ (권장) Spring 6+에서는 setCookieName() 대신 생성자로 쿠키 이름 지정
        // 이유: setCookieName(String)은 deprecated이며, 일부 환경에서 IDE가 메서드를 못 찾는 것처럼 표시되기도 함
        // 공식 권장: CookieLocaleResolver(String cookieName)
        CookieLocaleResolver clr = new CookieLocaleResolver("TABI_LANG"); // ← 여기서 쿠키 이름 설정 :contentReference[oaicite:1]{index=1}

        // ✅ 쿠키가 없을 때 기본 언어
        clr.setDefaultLocale(Locale.KOREA);

        // ✅ 쿠키 유지 기간
        // Spring 6에서는 Duration 버전이 권장됨 (Integer seconds 방식은 deprecated 쪽으로 이동)
        clr.setCookieMaxAge(Duration.ofDays(365));

        // (선택) 쿠키 적용 경로: 사이트 전체에서 언어 유지하려면 "/" 추천
        clr.setCookiePath("/");

        return clr;
    }

    @Bean
    public LocaleChangeInterceptor localeChangeInterceptor() {
        LocaleChangeInterceptor lci = new LocaleChangeInterceptor();
        lci.setParamName("lang"); // ?lang=ko|ja|en
        return lci;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(localeChangeInterceptor());
    }
}
