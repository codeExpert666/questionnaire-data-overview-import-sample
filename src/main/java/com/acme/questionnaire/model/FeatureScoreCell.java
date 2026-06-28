package com.acme.questionnaire.model;

import lombok.Data;

@Data
public class FeatureScoreCell {
    private Long answerId;
    private Long featureId;
    private Integer score;
}
