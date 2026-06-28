package com.acme.questionnaire.service;

import com.acme.questionnaire.dto.ScoreRowResponse;
import com.acme.questionnaire.dto.TablePageResponse;
import com.acme.questionnaire.dto.TableQueryRequest;
import com.acme.questionnaire.mapper.FeatureMapper;
import com.acme.questionnaire.mapper.QuestionnaireTableQueryMapper;
import com.acme.questionnaire.model.FeatureScoreCell;
import com.acme.questionnaire.model.ScoreQueryRow;
import com.acme.questionnaire.ref.FeatureRef;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QuestionnaireTableQueryServiceAssemblyTest {
    @Mock
    private QuestionnaireTableQueryMapper queryMapper;
    @Mock
    private FeatureMapper featureMapper;

    private QuestionnaireTableQueryService service;

    @BeforeEach
    void setUp() {
        service = new QuestionnaireTableQueryService(queryMapper, featureMapper);
        when(featureMapper.selectEnabledFeatures()).thenReturn(List.of(
                feature(1L, "续航", 10),
                feature(2L, "音质音效", 20)));
    }

    @Test
    void scoreQueryBuildsDynamicColumnsAndFeatureScoreCells() {
        ScoreQueryRow row = new ScoreQueryRow();
        row.setAnswerId(100L);
        row.setQuestionnaireId("Q001");
        row.setProductCode("P100");
        row.setProductModel("Alpha");
        row.setAnswerTime(LocalDateTime.of(2026, 6, 1, 9, 0));
        row.setRomVersion("1.0");
        row.setAppVersion("3.2");
        row.setRecommendScore(9);
        row.setUserCategory(3);

        FeatureScoreCell score = new FeatureScoreCell();
        score.setAnswerId(100L);
        score.setFeatureId(1L);
        score.setScore(8);

        when(queryMapper.countScores(any())).thenReturn(1L);
        when(queryMapper.selectScoreRows(any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(List.of(row));
        when(queryMapper.selectFeatureScoresByAnswerIds(List.of(100L)))
                .thenReturn(List.of(score));

        TablePageResponse<ScoreRowResponse> response = service.queryScores(
                new TableQueryRequest(1, 20, null, null, null));

        assertThat(response.columns()).extracting("key")
                .contains("questionnaireId", "recommendScore", "featureScore:1", "featureScore:2");
        assertThat(response.columns()).extracting("title")
                .contains("续航体验", "音质音效体验");
        assertThat(response.rows()).hasSize(1);
        assertThat(response.rows().get(0).getUserCategory()).isEqualTo("推荐者");
        assertThat(response.rows().get(0).getFlattenedFeatureScores())
                .containsEntry("featureScore:1", 8)
                .doesNotContainKey("featureScore:2");
    }

    private FeatureRef feature(Long id, String name, int sortNo) {
        FeatureRef feature = new FeatureRef();
        feature.setId(id);
        feature.setFeatureCode("F" + id);
        feature.setFeatureName(name);
        feature.setSortNo(sortNo);
        return feature;
    }
}
