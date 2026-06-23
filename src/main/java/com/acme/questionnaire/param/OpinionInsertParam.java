package com.acme.questionnaire.param;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class OpinionInsertParam {
    private Long answerId;
    private Integer opinionSeq;
    private Integer sentimentCode;
    private Long featureId;
    private String feedbackContent1;
    private String feedbackContent2;
}
