package org.hackathon.service;

import org.hackathon.data.enums.ResultCode;
import org.hackathon.exception.BusinessException;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class SubmissionTagService {
    private static final Map<String, String> ALIASES =
            Map.ofEntries(
                    Map.entry("cursor", "Cursor"),
                    Map.entry("claude", "Claude"),
                    Map.entry("claude code", "Claude Code"),
                    Map.entry("chatgpt", "ChatGPT"),
                    Map.entry("midjourney", "Midjourney"),
                    Map.entry("figma", "Figma"),
                    Map.entry("react", "React"),
                    Map.entry("vue", "Vue"),
                    Map.entry("vue.js", "Vue"),
                    Map.entry("spring boot", "Spring Boot"),
                    Map.entry("springboot", "Spring Boot"));

    public List<String> normalize(List<String> input, List<String> previous) {
        if (input == null) return previous;
        if (input.size() > 30) throw new BusinessException(ResultCode.PARAM_ERROR, "每类标签最多30项");
        TreeMap<String, String> unique = new TreeMap<>();
        for (String raw : input) {
            if (raw == null
                    || raw.isBlank()
                    || raw.length() > 50
                    || raw.chars().anyMatch(Character::isISOControl))
                throw new BusinessException(ResultCode.PARAM_ERROR, "标签必须为1至50个字符，且不含控制字符");
            String key = raw.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
            String canonical = ALIASES.getOrDefault(key, key);
            unique.put(canonical.toLowerCase(Locale.ROOT), canonical);
        }
        return List.copyOf(unique.values());
    }
}
