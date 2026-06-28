package com.acme.questionnaire.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class OpinionRowResponse {
    private Long opinionId;
    private Long answerId;
    private String questionnaireId;
    private String productModel;
    private String productCode;
    private LocalDateTime answerTime;
    private Integer recommendScore;
    private String userCategory;
    private Integer opinionSeq;
    private String featureName;
    private String sentiment;
    private String feedbackContent1;
    private String feedbackContent2;
    private String feedbackText;
    private String scoreReason;
}
