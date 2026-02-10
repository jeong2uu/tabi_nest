package com.tabi_nest.global.security.oauth;

import com.tabi_nest.user.domain.User;
import com.tabi_nest.user.repository.UserRepository;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;

/**
 * OAuth2 로그인으로 받은 사용자 정보를 users 테이블로 upsert 하는 서비스.
 *
 * 사용처:
 * - SecurityConfig.oauth2Login().userInfoEndpoint().userService(...)
 *
 * provider 값:
 * - L = LOCAL
 * - G = GOOGLE
 * - N = NAVER
 * - K = KAKAO
 *
 * NOTE:
 * - 각 제공자마다 user-info JSON 구조가 달라서 extract* 메서드로 분기 처리한다.
 */
@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    public CustomOAuth2UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        String registrationId = userRequest.getClientRegistration().getRegistrationId(); // google/naver/kakao
        Map<String, Object> attrs = oAuth2User.getAttributes();

        String provider = switch (registrationId) {
            case "google" -> "G";
            case "naver" -> "N";
            case "kakao" -> "K";
            default -> "L";
        };

        String providerId = extractId(registrationId, attrs);
        String email = extractEmail(registrationId, attrs);
        String name = extractName(registrationId, attrs);

        if (providerId != null) {
            Optional<User> existing = userRepository.findByProviderAndProviderId(provider, providerId);
            User user = existing.orElseGet(User::new);

            user.setProvider(provider);
            user.setProviderId(providerId);

            // OAuth는 이메일이 없는 경우도 있어서 null 체크
            if (email != null && !email.isBlank()) user.setEmail(email);
            if (name != null && !name.isBlank()) user.setName(name);

            // 기본값
            if (user.getRole() == null) user.setRole("USER");
            if (user.getStatus() == null) user.setStatus("A");

            userRepository.save(user);
        }

        return oAuth2User;
    }

    private String extractId(String reg, Map<String, Object> attrs) {
        try {
            if ("google".equals(reg)) {
                return (String) attrs.get("sub"); // OIDC subject
            }
            if ("kakao".equals(reg)) {
                Object id = attrs.get("id");
                return id != null ? String.valueOf(id) : null;
            }
            if ("naver".equals(reg)) {
                Object resp = attrs.get("response");
                if (resp instanceof Map<?, ?> m) {
                    Object id = m.get("id");
                    return id != null ? String.valueOf(id) : null;
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    private String extractEmail(String reg, Map<String, Object> attrs) {
        try {
            if ("google".equals(reg)) return (String) attrs.get("email");
            if ("kakao".equals(reg)) {
                Object account = attrs.get("kakao_account");
                if (account instanceof Map<?, ?> m) return (String) m.get("email");
            }
            if ("naver".equals(reg)) {
                Object resp = attrs.get("response");
                if (resp instanceof Map<?, ?> m) return (String) m.get("email");
            }
        } catch (Exception ignored) {}
        return null;
    }

    private String extractName(String reg, Map<String, Object> attrs) {
        try {
            if ("google".equals(reg)) return (String) attrs.get("name");
            if ("kakao".equals(reg)) {
                Object account = attrs.get("kakao_account");
                if (account instanceof Map<?, ?> m) {
                    Object profile = m.get("profile");
                    if (profile instanceof Map<?, ?> p) return (String) p.get("nickname");
                }
            }
            if ("naver".equals(reg)) {
                Object resp = attrs.get("response");
                if (resp instanceof Map<?, ?> m) return (String) m.get("name");
            }
        } catch (Exception ignored) {}
        return null;
    }
}
