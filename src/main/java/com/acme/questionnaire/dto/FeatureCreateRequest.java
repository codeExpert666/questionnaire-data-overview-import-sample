package com.acme.questionnaire.dto;

public record FeatureCreateRequest(
        String featureCode,
        String featureName,
        Integer sortNo,
        Integer status
) {
}
