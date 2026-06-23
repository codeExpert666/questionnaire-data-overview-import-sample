package com.acme.questionnaire.dto;

public record FeatureUpdateRequest(
        String featureName,
        Integer sortNo
) {
}
