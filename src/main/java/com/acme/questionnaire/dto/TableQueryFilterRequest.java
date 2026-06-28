package com.acme.questionnaire.dto;

import java.time.LocalDateTime;

public record TableQueryFilterRequest(
        String questionnaireId,
        String productCode,
        String productModel,
        LocalDateTime answerTimeStart,
        LocalDateTime answerTimeEnd,
        String romVersion,
        String appVersion,
        Integer recommendScoreMin,
        Integer recommendScoreMax,
        Integer userCategory,
        Integer sentiment,
        Long featureId,
        String keyword
) {
}
