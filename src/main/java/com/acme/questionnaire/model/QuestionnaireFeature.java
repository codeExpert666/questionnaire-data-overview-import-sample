package com.acme.questionnaire.model;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class QuestionnaireFeature {
    private Long id;
    private String featureCode;
    private String featureName;
    private Integer sortNo;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
