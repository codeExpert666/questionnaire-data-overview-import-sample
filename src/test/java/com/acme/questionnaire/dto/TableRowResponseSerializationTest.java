package com.acme.questionnaire.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class TableRowResponseSerializationTest {
    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    @Test
    void scoreRowSerializesDynamicFeatureScoreAsTopLevelField() throws Exception {
        ScoreRowResponse row = new ScoreRowResponse();
        row.setAnswerId(100L);
        row.setQuestionnaireId("Q001");
        row.setProductCode("P100");
        row.setProductModel("Alpha");
        row.setAnswerTime(LocalDateTime.of(2026, 6, 1, 9, 30));
        row.setRecommendScore(9);
        row.setUserCategory("推荐者");
        row.putFeatureScore(7L, 8);

        JsonNode json = objectMapper.readTree(objectMapper.writeValueAsString(row));

        assertThat(json.get("featureScore:7").asInt()).isEqualTo(8);
        assertThat(json.has("featureScores")).isFalse();
        assertThat(json.get("questionnaireId").asText()).isEqualTo("Q001");
    }

    @Test
    void dataOverviewRowSerializesDynamicFeatureScoreAsTopLevelField() throws Exception {
        DataOverviewRowResponse row = new DataOverviewRowResponse();
        row.setAnswerId(100L);
        row.setOpinionId(200L);
        row.setQuestionnaireId("Q001");
        row.setSentiment("好评");
        row.setFeatureCategoryName("物流包装");
        row.putFeatureScore(3L, 10);

        JsonNode json = objectMapper.readTree(objectMapper.writeValueAsString(row));

        assertThat(json.get("featureScore:3").asInt()).isEqualTo(10);
        assertThat(json.has("featureScores")).isFalse();
        assertThat(json.get("featureCategoryName").asText()).isEqualTo("物流包装");
        assertThat(json.get("sentiment").asText()).isEqualTo("好评");
    }
}
