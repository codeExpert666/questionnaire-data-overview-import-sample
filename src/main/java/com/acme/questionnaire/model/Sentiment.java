package com.acme.questionnaire.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

@Getter
@RequiredArgsConstructor
public enum Sentiment {
    NO_FEEDBACK(0, "未反馈"),
    NEGATIVE(1, "差评"),
    NEUTRAL(2, "中评"),
    POSITIVE(3, "好评");

    private final int code;
    private final String displayName;

    public static Sentiment fromText(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("情感观点不能为空");
        }
        String normalized = value.trim();
        return Arrays.stream(values())
                .filter(item -> item.displayName.equals(normalized)
                        || String.valueOf(item.code).equals(normalized)
                        || item.name().equalsIgnoreCase(normalized))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "情感观点仅支持：好评、中评、差评、未反馈"));
    }
}
