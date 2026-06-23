package com.acme.questionnaire.param;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class AnswerUpsertParam {
    private String sourceSystem;
    private String questionnaireId;
    private Long productId;
    private LocalDateTime answerTime;
    private String romVersion;
    private String appVersion;
    private String feedbackText;
    private String scoreReason;
    private Integer recommendScore;
    private Integer userCategory;
}
