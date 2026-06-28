package com.acme.questionnaire.model;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ScoreQueryRow {
    private Long answerId;
    private String questionnaireId;
    private String productModel;
    private String productCode;
    private LocalDateTime answerTime;
    private String romVersion;
    private String appVersion;
    private Integer recommendScore;
    private Integer userCategory;
}
