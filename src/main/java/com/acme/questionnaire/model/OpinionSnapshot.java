package com.acme.questionnaire.model;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class OpinionSnapshot {
    int rowNumber;
    Sentiment sentiment;
    Long featureId;
    String feedbackContent1;
    String feedbackContent2;
}
