package com.acme.questionnaire.dto;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Getter
@Setter
public class ScoreRowResponse {
    private Long answerId;
    private String questionnaireId;
    private String productModel;
    private String productCode;
    private LocalDateTime answerTime;
    private String romVersion;
    private String appVersion;
    private Integer recommendScore;
    private String userCategory;

    @JsonIgnore
    private final Map<String, Integer> featureScores = new LinkedHashMap<>();

    public void putFeatureScore(Long featureId, Integer score) {
        featureScores.put("featureScore:" + featureId, score);
    }

    @JsonAnyGetter
    public Map<String, Integer> getFlattenedFeatureScores() {
        return featureScores;
    }
}
