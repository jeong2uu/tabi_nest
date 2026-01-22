package com.tabi_nest.common;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import org.springframework.context.MessageSource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RegionApiController {

    private final MessageSource messageSource;

    public RegionApiController(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    @GetMapping("/api/regions/names")
    public Map<String, String> regionNames(Locale locale) {
        Map<String, String> map = new LinkedHashMap<>();
        for (int i = 1; i <= 47; i++) {
            String id = String.format("JP-%02d", i);
            String key = "region." + id + ".name";
            String val = messageSource.getMessage(key, null, id, locale);
            map.put(id, val);
        }
        return map;
    }

    @GetMapping("/api/regions/descs")
    public Map<String, String> regionDescs(Locale locale) {
        Map<String, String> map = new LinkedHashMap<>();
        for (int i = 1; i <= 47; i++) {
            String id = String.format("JP-%02d", i);
            String key = "region." + id + ".desc";
            String val = messageSource.getMessage(key, null, "", locale);
            map.put(id, val);
        }
        return map;
    }

    private Locale resolveLocale(String lang, Locale fallback){
        if (lang == null || lang.isBlank()) return fallback;
        String v = lang.trim().toLowerCase(Locale.ROOT);
        return switch (v){
            case "ja" -> Locale.JAPAN;
            case "en" -> Locale.ENGLISH;
            default -> Locale.KOREA;
        };
    }
}
