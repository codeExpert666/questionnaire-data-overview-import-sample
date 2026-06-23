package com.acme.questionnaire.dto;

import com.acme.questionnaire.model.QuestionnaireFeature;

import java.time.LocalDateTime;

public record FeatureResponse(
        Long id,
        String featureCode,
        String featureName,
        Integer sortNo,
        Integer status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static FeatureResponse from(QuestionnaireFeature feature) {
        return new FeatureResponse(
                feature.getId(),
                feature.getFeatureCode(),
                feature.getFeatureName(),
                feature.getSortNo(),
                feature.getStatus(),
                feature.getCreatedAt(),
                feature.getUpdatedAt());
    }
}
