package com.acme.questionnaire.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

@Getter
@RequiredArgsConstructor
public enum UserCategory {
    UNKNOWN(0, "未知"),
    DETRACTOR(1, "贬损者"),
    PASSIVE(2, "中立者"),
    PROMOTER(3, "推荐者");

    private final int code;
    private final String displayName;

    public static UserCategory fromText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim();
        return Arrays.stream(values())
                .filter(item -> item.displayName.equals(normalized)
                        || String.valueOf(item.code).equals(normalized)
                        || item.name().equalsIgnoreCase(normalized))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "用户归类仅支持：推荐者、中立者、贬损者、未知"));
    }
}
