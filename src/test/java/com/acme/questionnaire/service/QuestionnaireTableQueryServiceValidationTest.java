package com.acme.questionnaire.service;

import com.acme.questionnaire.dto.FeatureScoreFilterRequest;
import com.acme.questionnaire.dto.TableQueryFilterRequest;
import com.acme.questionnaire.dto.TableQueryRequest;
import com.acme.questionnaire.dto.TableSortRequest;
import com.acme.questionnaire.exception.QuestionnaireQueryException;
import com.acme.questionnaire.mapper.FeatureMapper;
import com.acme.questionnaire.mapper.QuestionnaireTableQueryMapper;
import com.acme.questionnaire.model.FeatureScoreFilterCriteria;
import com.acme.questionnaire.model.FeatureScoreSortClause;
import com.acme.questionnaire.model.TableOrderClause;
import com.acme.questionnaire.model.TableQueryCriteria;
import com.acme.questionnaire.ref.FeatureRef;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QuestionnaireTableQueryServiceValidationTest {
    @Mock
    private QuestionnaireTableQueryMapper queryMapper;
    @Mock
    private FeatureMapper featureMapper;

    private QuestionnaireTableQueryService service;

    @BeforeEach
    void setUp() {
        service = new QuestionnaireTableQueryService(queryMapper, featureMapper);
        lenient().when(featureMapper.selectEnabledFeatures()).thenReturn(List.of(feature(1L, "续航", 10)));
    }

    @Test
    void scoreQueryDefaultsPageAndCapsPageSize() {
        when(queryMapper.countScores(any())).thenReturn(1L);
        when(queryMapper.selectScoreRows(any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(List.of());

        service.queryScores(new TableQueryRequest(
                0,
                500,
                null,
                null,
                null));

        ArgumentCaptor<Integer> offset = ArgumentCaptor.forClass(Integer.class);
        ArgumentCaptor<Integer> limit = ArgumentCaptor.forClass(Integer.class);
        verify(queryMapper).selectScoreRows(
                any(),
                any(),
                any(),
                offset.capture(),
                limit.capture());
        assertThat(offset.getValue()).isZero();
        assertThat(limit.getValue()).isEqualTo(200);
    }

    @Test
    void rejectsOffsetOverflow() {
        TableQueryRequest request = new TableQueryRequest(
                Integer.MAX_VALUE,
                200,
                null,
                null,
                null);

        assertThatThrownBy(() -> service.queryScores(request))
                .isInstanceOf(QuestionnaireQueryException.class)
                .hasMessageContaining("分页偏移量超出限制");
    }

    @Test
    @SuppressWarnings("unchecked")
    void dynamicFeatureSortUsesSafeAliasAndOrderClause() {
        when(queryMapper.countScores(any())).thenReturn(1L);
        when(queryMapper.selectScoreRows(any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(List.of());

        service.queryScores(new TableQueryRequest(
                1,
                20,
                null,
                null,
                List.of(new TableSortRequest("featureScore:1", "desc"))));

        ArgumentCaptor<List<TableOrderClause>> orderClauses =
                ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<List<FeatureScoreSortClause>> featureScoreSorts =
                ArgumentCaptor.forClass(List.class);
        verify(queryMapper).selectScoreRows(
                any(),
                orderClauses.capture(),
                featureScoreSorts.capture(),
                anyInt(),
                anyInt());
        assertThat(featureScoreSorts.getValue())
                .containsExactly(new FeatureScoreSortClause("sort_score_0", 1L));
        assertThat(orderClauses.getValue())
                .containsExactly(
                        new TableOrderClause("sort_score_0.score", "DESC"),
                        new TableOrderClause("a.id", "ASC"));
    }

    @Test
    void criteriaNormalizationTrimsStringsAndPreservesCodesAndFeatureScoreFilter() {
        when(queryMapper.countDataOverview(any())).thenReturn(0L);

        service.queryDataOverview(new TableQueryRequest(
                1,
                20,
                new TableQueryFilterRequest(
                        " Q001 ",
                        "   ",
                        " Alpha ",
                        null,
                        null,
                        " ROM-1 ",
                        "",
                        2,
                        9,
                        3,
                        1,
                        null,
                        "  包装  ",
                        "  续航  "),
                List.of(new FeatureScoreFilterRequest(1L, null, null)),
                null));

        ArgumentCaptor<TableQueryCriteria> criteria =
                ArgumentCaptor.forClass(TableQueryCriteria.class);
        verify(queryMapper).countDataOverview(criteria.capture());
        TableQueryCriteria normalized = criteria.getValue();
        assertThat(normalized.getQuestionnaireId()).isEqualTo("Q001");
        assertThat(normalized.getProductCode()).isNull();
        assertThat(normalized.getProductModel()).isEqualTo("Alpha");
        assertThat(normalized.getRomVersion()).isEqualTo("ROM-1");
        assertThat(normalized.getAppVersion()).isNull();
        assertThat(normalized.getRecommendScoreMin()).isEqualTo(2);
        assertThat(normalized.getRecommendScoreMax()).isEqualTo(9);
        assertThat(normalized.getUserCategory()).isEqualTo(3);
        assertThat(normalized.getSentiment()).isEqualTo(1);
        assertThat(normalized.getFeatureId()).isNull();
        assertThat(normalized.getFeatureCategoryName()).isEqualTo("包装");
        assertThat(normalized.getKeyword()).isEqualTo("续航");
        assertThat(normalized.getFeatureScoreFilters())
                .containsExactly(new FeatureScoreFilterCriteria(1L, null, null));
    }

    @Test
    void scoreQueryRejectsOpinionOnlyFilters() {
        TableQueryRequest sentimentRequest = new TableQueryRequest(
                1,
                20,
                new TableQueryFilterRequest(
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        1,
                        null,
                        null,
                        null),
                null,
                null);
        assertThatThrownBy(() -> service.queryScores(sentimentRequest))
                .isInstanceOf(QuestionnaireQueryException.class)
                .hasMessageContaining("评分查询不支持情感观点过滤");

        TableQueryRequest keywordRequest = new TableQueryRequest(
                1,
                20,
                new TableQueryFilterRequest(
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        "续航"),
                null,
                null);
        assertThatThrownBy(() -> service.queryScores(keywordRequest))
                .isInstanceOf(QuestionnaireQueryException.class)
                .hasMessageContaining("评分查询不支持关键词过滤");
    }

    @Test
    void opinionsRejectFeatureScoreFiltersAndDynamicFeatureScoreSorts() {
        TableQueryRequest filterRequest = new TableQueryRequest(
                1,
                20,
                null,
                List.of(new FeatureScoreFilterRequest(1L, null, null)),
                null);
        assertThatThrownBy(() -> service.queryOpinions(filterRequest))
                .isInstanceOf(QuestionnaireQueryException.class)
                .hasMessageContaining("观点查询不支持特性评分过滤");

        TableQueryRequest sortRequest = new TableQueryRequest(
                1,
                20,
                null,
                null,
                List.of(new TableSortRequest("featureScore:1", "asc")));
        assertThatThrownBy(() -> service.queryOpinions(sortRequest))
                .isInstanceOf(QuestionnaireQueryException.class)
                .hasMessageContaining("观点查询不支持特性评分排序");
    }

    @Test
    @SuppressWarnings("unchecked")
    void staticSortDirectionIsCaseInsensitive() {
        when(queryMapper.countScores(any())).thenReturn(1L);
        when(queryMapper.selectScoreRows(any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(List.of());

        service.queryScores(new TableQueryRequest(
                1,
                20,
                null,
                null,
                List.of(new TableSortRequest("answerTime", "dEsC"))));

        ArgumentCaptor<List<TableOrderClause>> orderClauses =
                ArgumentCaptor.forClass(List.class);
        verify(queryMapper).selectScoreRows(
                any(),
                orderClauses.capture(),
                any(),
                anyInt(),
                anyInt());
        assertThat(orderClauses.getValue())
                .containsExactly(
                        new TableOrderClause("a.answer_time", "DESC"),
                        new TableOrderClause("a.id", "ASC"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void dataOverviewCustomSortsAppendStableOpinionTail() {
        when(queryMapper.countDataOverview(any())).thenReturn(1L);
        when(queryMapper.selectDataOverviewRows(any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(List.of());

        service.queryDataOverview(new TableQueryRequest(
                1,
                20,
                null,
                null,
                List.of(new TableSortRequest("featureCategoryName", "desc"))));

        ArgumentCaptor<List<TableOrderClause>> orderClauses =
                ArgumentCaptor.forClass(List.class);
        verify(queryMapper).selectDataOverviewRows(
                any(),
                orderClauses.capture(),
                any(),
                anyInt(),
                anyInt());
        assertThat(orderClauses.getValue())
                .containsExactly(
                        new TableOrderClause("o.feature_category_name", "DESC"),
                        new TableOrderClause("a.id", "ASC"),
                        new TableOrderClause("o.opinion_seq", "ASC"),
                        new TableOrderClause("o.id", "ASC"));
    }

    @Test
    void opinionGrainQueriesRejectFeatureIdFilter() {
        TableQueryRequest request = new TableQueryRequest(
                1,
                20,
                new TableQueryFilterRequest(
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        1L,
                        null,
                        null),
                null,
                null);

        assertThatThrownBy(() -> service.queryDataOverview(request))
                .isInstanceOf(QuestionnaireQueryException.class)
                .hasMessageContaining("数据总览和观点查询不支持特性ID过滤");
        assertThatThrownBy(() -> service.queryOpinions(request))
                .isInstanceOf(QuestionnaireQueryException.class)
                .hasMessageContaining("数据总览和观点查询不支持特性ID过滤");
    }

    @Test
    @SuppressWarnings("unchecked")
    void opinionCustomSortsAppendStableOpinionTailWithoutDuplicates() {
        when(queryMapper.countOpinions(any())).thenReturn(1L);
        when(queryMapper.selectOpinionRows(any(), any(), anyInt(), anyInt()))
                .thenReturn(List.of());

        service.queryOpinions(new TableQueryRequest(
                1,
                20,
                null,
                null,
                List.of(new TableSortRequest("opinionSeq", "desc"))));

        ArgumentCaptor<List<TableOrderClause>> orderClauses =
                ArgumentCaptor.forClass(List.class);
        verify(queryMapper).selectOpinionRows(
                any(),
                orderClauses.capture(),
                anyInt(),
                anyInt());
        assertThat(orderClauses.getValue())
                .containsExactly(
                        new TableOrderClause("o.opinion_seq", "DESC"),
                        new TableOrderClause("a.id", "ASC"),
                        new TableOrderClause("o.id", "ASC"));
    }

    @Test
    void rejectsInvalidSortDirection() {
        TableQueryRequest request = new TableQueryRequest(
                1,
                20,
                null,
                null,
                List.of(new TableSortRequest("answerTime", "sideways")));

        assertThatThrownBy(() -> service.queryScores(request))
                .isInstanceOf(QuestionnaireQueryException.class)
                .hasMessageContaining("排序方向只支持 asc 或 desc");
    }

    @Test
    void rejectsUnknownSortField() {
        TableQueryRequest request = new TableQueryRequest(
                1,
                20,
                null,
                null,
                List.of(new TableSortRequest("a.id desc", "desc")));

        assertThatThrownBy(() -> service.queryScores(request))
                .isInstanceOf(QuestionnaireQueryException.class)
                .hasMessageContaining("不支持的排序字段");
    }

    @Test
    void rejectsDynamicFeatureSortWhenFeatureIsDisabledOrMissing() {
        TableQueryRequest request = new TableQueryRequest(
                1,
                20,
                null,
                null,
                List.of(new TableSortRequest("featureScore:99", "asc")));

        assertThatThrownBy(() -> service.queryScores(request))
                .isInstanceOf(QuestionnaireQueryException.class)
                .hasMessageContaining("特性不存在或已停用");
    }

    @Test
    void rejectsScoreRangeOutsideOneToTen() {
        TableQueryRequest request = new TableQueryRequest(
                1,
                20,
                new TableQueryFilterRequest(
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        0,
                        10,
                        null,
                        null,
                        null,
                        null,
                        null),
                null,
                null);

        assertThatThrownBy(() -> service.queryScores(request))
                .isInstanceOf(QuestionnaireQueryException.class)
                .hasMessageContaining("推荐意愿评分范围必须在1到10之间");
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
