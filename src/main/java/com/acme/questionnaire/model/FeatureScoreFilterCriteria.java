package com.acme.questionnaire.model;

import lombok.Value;

@Value
public class FeatureScoreFilterCriteria {
    Long featureId;
    Integer min;
    Integer max;
}
