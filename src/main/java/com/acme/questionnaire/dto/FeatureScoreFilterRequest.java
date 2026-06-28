package com.acme.questionnaire.dto;

public record FeatureScoreFilterRequest(
        Long featureId,
        Integer min,
        Integer max
) {
}
