package com.tabi_nest.common;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
public class I18nApiController {

    @GetMapping("/api/i18n")
    public Map<String, String> getBundle(@RequestParam(defaultValue = "ko") String lang) {
        Locale locale = toLocale(lang);

        // ResourceBundle name must match messages*.properties
        ResourceBundle bundle = ResourceBundle.getBundle("messages", locale);

        Map<String, String> map = new LinkedHashMap<>();
        for (String key : bundle.keySet()) {
            map.put(key, bundle.getString(key));
        }
        return map;
    }

    /**
     * Sets the TABI_LANG cookie so server-side endpoints that rely on LocaleResolver
     * can use the same language even without page reload.
     */
    @PostMapping("/api/i18n/lang")
    public void setLangCookie(@RequestParam(defaultValue = "ko") String lang, HttpServletResponse response) {
        String value = normalize(lang);
        response.addHeader("Set-Cookie", "TABI_LANG=" + value + "; Path=/; Max-Age=31536000; SameSite=Lax");
        response.setStatus(204);
    }

    private static Locale toLocale(String lang) {
        return switch (normalize(lang)) {
            case "ja" -> Locale.JAPAN;
            case "en" -> Locale.ENGLISH;
            default -> Locale.KOREA;
        };
    }

    private static String normalize(String lang) {
        return lang == null ? "ko" : lang.trim().toLowerCase(Locale.ROOT);
    }
}
