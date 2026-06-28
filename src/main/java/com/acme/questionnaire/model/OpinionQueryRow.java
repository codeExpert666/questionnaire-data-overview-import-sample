package com.acme.questionnaire.model;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class OpinionQueryRow {
    private Long opinionId;
    private Long answerId;
    private String questionnaireId;
    private String productModel;
    private String productCode;
    private LocalDateTime answerTime;
    private Integer recommendScore;
    private Integer userCategory;
    private Integer opinionSeq;
    private String featureName;
    private Integer sentiment;
    private String feedbackContent1;
    private String feedbackContent2;
    private String feedbackText;
    private String scoreReason;
}
