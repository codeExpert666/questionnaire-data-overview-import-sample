package com.acme.questionnaire.model;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;
import java.util.Map;

@Value
@Builder
public class AnswerSnapshot {
    int firstRowNumber;
    String questionnaireId;
    Long productId;
    String productCode;
    String productModel;
    LocalDateTime answerTime;
    String romVersion;
    String appVersion;
    String feedbackText;
    String scoreReason;
    Integer recommendScore;
    UserCategory userCategory;
    Map<Long, Integer> featureScores;
}
