package com.acme.questionnaire.param;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class FeatureScoreInsertParam {
    private Long answerId;
    private Long featureId;
    private Integer score;
}
