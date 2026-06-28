package com.acme.questionnaire.model;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;
import java.util.List;

@Value
@Builder
public class TableQueryCriteria {
    String questionnaireId;
    String productCode;
    String productModel;
    LocalDateTime answerTimeStart;
    LocalDateTime answerTimeEnd;
    String romVersion;
    String appVersion;
    Integer recommendScoreMin;
    Integer recommendScoreMax;
    Integer userCategory;
    Integer sentiment;
    Long featureId;
    String keyword;
    List<FeatureScoreFilterCriteria> featureScoreFilters;
}
