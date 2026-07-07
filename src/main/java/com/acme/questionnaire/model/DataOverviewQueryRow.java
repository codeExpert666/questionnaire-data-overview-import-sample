package com.acme.questionnaire.model;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class DataOverviewQueryRow {
    private Long answerId;
    private Long opinionId;
    private String questionnaireId;
    private String productModel;
    private String productCode;
    private LocalDateTime answerTime;
    private String romVersion;
    private String appVersion;
    private String feedbackText;
    private String scoreReason;
    private Integer recommendScore;
    private Integer userCategory;
    private Integer sentiment;
    private String featureCategoryName;
    private String feedbackContent1;
    private String feedbackContent2;
}
