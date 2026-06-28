package com.acme.questionnaire.service;

import com.acme.questionnaire.dto.DataOverviewRowResponse;
import com.acme.questionnaire.dto.OpinionRowResponse;
import com.acme.questionnaire.dto.ScoreRowResponse;
import com.acme.questionnaire.dto.TableColumnResponse;
import com.acme.questionnaire.dto.TablePageResponse;
import com.acme.questionnaire.dto.TableQueryRequest;
import com.acme.questionnaire.mapper.FeatureMapper;
import com.acme.questionnaire.mapper.QuestionnaireTableQueryMapper;
import com.acme.questionnaire.model.DataOverviewQueryRow;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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
        when(queryMapper.countScores(any())).thenReturn(1L);
        when(queryMapper.selectScoreRows(any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(List.of(scoreRow(100L)));
        when(queryMapper.selectFeatureScoresByAnswerIds(List.of(100L)))
                .thenReturn(List.of(featureScore(100L, 1L, 8)));

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

    @Test
    void scoreQueryIgnoresReturnedScoreCellsForDisabledFeatures() {
        when(queryMapper.countScores(any())).thenReturn(1L);
        when(queryMapper.selectScoreRows(any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(List.of(scoreRow(100L)));
        when(queryMapper.selectFeatureScoresByAnswerIds(List.of(100L)))
                .thenReturn(List.of(
                        featureScore(100L, 1L, 8),
                        featureScore(100L, 99L, 7)));

        TablePageResponse<ScoreRowResponse> response = service.queryScores(
                new TableQueryRequest(1, 20, null, null, null));

        assertThat(response.columns()).extracting("key")
                .contains("featureScore:1", "featureScore:2")
                .doesNotContain("featureScore:99");
        assertThat(response.rows().get(0).getFlattenedFeatureScores())
                .containsEntry("featureScore:1", 8)
                .doesNotContainKey("featureScore:99");
    }

    @Test
    void dataOverviewRowsSharingAnswerIdUseTheSameFetchedFeatureScores() {
        when(queryMapper.countDataOverview(any())).thenReturn(2L);
        when(queryMapper.selectDataOverviewRows(any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(List.of(
                        dataOverviewRow(100L, 201L),
                        dataOverviewRow(100L, 202L)));
        when(queryMapper.selectFeatureScoresByAnswerIds(List.of(100L)))
                .thenReturn(List.of(featureScore(100L, 1L, 8)));

        TablePageResponse<DataOverviewRowResponse> response = service.queryDataOverview(
                new TableQueryRequest(1, 20, null, null, null));

        assertThat(response.rows()).hasSize(2);
        assertThat(response.rows())
                .allSatisfy(row -> assertThat(row.getFlattenedFeatureScores())
                        .containsEntry("featureScore:1", 8));
        verify(queryMapper).selectFeatureScoresByAnswerIds(List.of(100L));
    }

    @Test
    void rowsWithoutAnswerIdsDoNotFetchFeatureScores() {
        when(queryMapper.countScores(any())).thenReturn(1L);
        when(queryMapper.selectScoreRows(any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(List.of(scoreRow(null)));
        when(queryMapper.countDataOverview(any())).thenReturn(1L);
        when(queryMapper.selectDataOverviewRows(any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(List.of(dataOverviewRow(null, 201L)));

        service.queryScores(new TableQueryRequest(1, 20, null, null, null));
        service.queryDataOverview(new TableQueryRequest(1, 20, null, null, null));

        verify(queryMapper, never()).selectFeatureScoresByAnswerIds(any());
    }

    @Test
    void opinionColumnsDoNotIncludeDynamicFeatureScoreColumns() {
        when(queryMapper.countOpinions(any())).thenReturn(0L);

        TablePageResponse<OpinionRowResponse> response = service.queryOpinions(
                new TableQueryRequest(1, 20, null, null, null));

        assertThat(response.columns()).extracting("key")
                .doesNotContain("featureScore:1", "featureScore:2");
    }

    @Test
    void columnCapabilitiesMatchSupportedSortAndFilterContract() {
        when(queryMapper.countDataOverview(any())).thenReturn(0L);

        TablePageResponse<DataOverviewRowResponse> response = service.queryDataOverview(
                new TableQueryRequest(1, 20, null, null, null));

        assertThat(column(response, "questionnaireId").sortable()).isTrue();
        assertThat(column(response, "questionnaireId").filterable()).isTrue();
        assertThat(column(response, "sentiment").sortable()).isTrue();
        assertThat(column(response, "sentiment").filterable()).isTrue();
        assertThat(column(response, "featureName").sortable()).isTrue();
        assertThat(column(response, "featureName").filterable()).isTrue();
        assertThat(column(response, "feedbackText").sortable()).isFalse();
        assertThat(column(response, "feedbackText").filterable()).isFalse();
        assertThat(column(response, "scoreReason").sortable()).isFalse();
        assertThat(column(response, "scoreReason").filterable()).isFalse();
        assertThat(column(response, "feedbackContent1").sortable()).isFalse();
        assertThat(column(response, "feedbackContent1").filterable()).isFalse();
        assertThat(column(response, "featureScore:1").sortable()).isTrue();
        assertThat(column(response, "featureScore:1").filterable()).isTrue();

        when(queryMapper.countOpinions(any())).thenReturn(0L);
        TablePageResponse<OpinionRowResponse> opinionResponse = service.queryOpinions(
                new TableQueryRequest(1, 20, null, null, null));
        assertThat(column(opinionResponse, "featureName").sortable()).isTrue();
        assertThat(column(opinionResponse, "featureName").filterable()).isTrue();
    }

    private ScoreQueryRow scoreRow(Long answerId) {
        ScoreQueryRow row = new ScoreQueryRow();
        row.setAnswerId(answerId);
        row.setQuestionnaireId("Q001");
        row.setProductCode("P100");
        row.setProductModel("Alpha");
        row.setAnswerTime(LocalDateTime.of(2026, 6, 1, 9, 0));
        row.setRomVersion("1.0");
        row.setAppVersion("3.2");
        row.setRecommendScore(9);
        row.setUserCategory(3);
        return row;
    }

    private DataOverviewQueryRow dataOverviewRow(Long answerId, Long opinionId) {
        DataOverviewQueryRow row = new DataOverviewQueryRow();
        row.setAnswerId(answerId);
        row.setOpinionId(opinionId);
        row.setQuestionnaireId("Q001");
        row.setProductCode("P100");
        row.setProductModel("Alpha");
        row.setAnswerTime(LocalDateTime.of(2026, 6, 1, 9, 0));
        row.setRomVersion("1.0");
        row.setAppVersion("3.2");
        row.setFeedbackText("续航不错");
        row.setScoreReason("体验稳定");
        row.setRecommendScore(9);
        row.setUserCategory(3);
        row.setSentiment(3);
        row.setFeatureName("续航");
        row.setFeedbackContent1("续航时间长");
        row.setFeedbackContent2("充电速度快");
        return row;
    }

    private FeatureScoreCell featureScore(Long answerId, Long featureId, Integer score) {
        FeatureScoreCell cell = new FeatureScoreCell();
        cell.setAnswerId(answerId);
        cell.setFeatureId(featureId);
        cell.setScore(score);
        return cell;
    }

    private TableColumnResponse column(TablePageResponse<?> response, String key) {
        return response.columns().stream()
                .filter(column -> key.equals(column.key()))
                .findFirst()
                .orElseThrow();
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
