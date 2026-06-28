package com.acme.questionnaire.service;

import com.acme.questionnaire.dto.TableQueryFilterRequest;
import com.acme.questionnaire.dto.TableQueryRequest;
import com.acme.questionnaire.dto.TableSortRequest;
import com.acme.questionnaire.exception.QuestionnaireQueryException;
import com.acme.questionnaire.mapper.FeatureMapper;
import com.acme.questionnaire.mapper.QuestionnaireTableQueryMapper;
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
        when(featureMapper.selectEnabledFeatures()).thenReturn(List.of(feature(1L, "续航", 10)));
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
