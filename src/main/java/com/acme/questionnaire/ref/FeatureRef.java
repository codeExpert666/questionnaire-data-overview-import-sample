package com.acme.questionnaire.ref;

import lombok.Data;

@Data
public class FeatureRef {
    private Long id;
    private String featureCode;
    private String featureName;
    private Integer sortNo;
}
