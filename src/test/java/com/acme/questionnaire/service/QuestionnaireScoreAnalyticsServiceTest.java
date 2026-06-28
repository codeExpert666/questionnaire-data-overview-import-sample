package com.acme.questionnaire.service;

import com.acme.questionnaire.dto.NssFeatureAnalyticsRequest;
import com.acme.questionnaire.dto.NssFeatureScoreResponse;
import com.acme.questionnaire.dto.ScoreAnalyticsFilterOptionsResponse;
import com.acme.questionnaire.dto.ScoreAnalyticsRequest;
import com.acme.questionnaire.mapper.FeatureMapper;
import com.acme.questionnaire.mapper.QuestionnaireScoreAnalyticsMapper;
import com.acme.questionnaire.model.AnalyticsProductOptionRow;
import com.acme.questionnaire.model.NpsAnalyticsBucketRow;
import com.acme.questionnaire.model.NpsAnalyticsOverviewRow;
import com.acme.questionnaire.model.NssFeatureAnalyticsRow;
import com.acme.questionnaire.model.ScoreAnalyticsCriteria;
import com.acme.questionnaire.ref.FeatureRef;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QuestionnaireScoreAnalyticsServiceTest {
    @Mock
    private QuestionnaireScoreAnalyticsMapper analyticsMapper;
    @Mock
    private FeatureMapper featureMapper;

    private QuestionnaireScoreAnalyticsService service;

    @BeforeEach
    void setUp() {
        Clock fixedClock = Clock.fixed(
                Instant.parse("2026-06-29T01:00:00Z"),
                ZoneId.of("Asia/Shanghai"));
        service = new QuestionnaireScoreAnalyticsService(analyticsMapper, featureMapper, fixedClock);
    }

    @Test
    void npsOverviewUsesDefaultThreeMonthRangeAndFormatsScores() {
        NpsAnalyticsOverviewRow overviewRow = new NpsAnalyticsOverviewRow();
        overviewRow.setPromoterCount(2L);
        overviewRow.setPassiveCount(1L);
        overviewRow.setDetractorCount(1L);
        overviewRow.setAverageRecommendScore(new BigDecimal("8.25"));
        when(analyticsMapper.selectNpsOverview(any())).thenReturn(overviewRow);

        var response = service.queryNpsOverview(new ScoreAnalyticsRequest(
                null,
                null,
                List.of(" Alpha ", "", "Alpha"),
                List.of(" 8.1.0 "),
                List.of(),
                null));

        assertThat(response.npsScore()).isEqualByComparingTo("25");
        assertThat(response.npsScore().scale()).isZero();
        assertThat(response.averageRecommendScore()).isEqualByComparingTo("8.3");
        assertThat(response.distribution().promoterCount()).isEqualTo(2);
        assertThat(response.distribution().passiveCount()).isEqualTo(1);
        assertThat(response.distribution().detractorCount()).isEqualTo(1);

        ArgumentCaptor<ScoreAnalyticsCriteria> criteriaCaptor =
                ArgumentCaptor.forClass(ScoreAnalyticsCriteria.class);
        verify(analyticsMapper).selectNpsOverview(criteriaCaptor.capture());
        ScoreAnalyticsCriteria criteria = criteriaCaptor.getValue();
        assertThat(criteria.getStartDate()).isEqualTo(LocalDate.of(2026, 3, 29));
        assertThat(criteria.getEndDate()).isEqualTo(LocalDate.of(2026, 6, 29));
        assertThat(criteria.getStartTimeInclusive()).isEqualTo(LocalDateTime.of(2026, 3, 29, 0, 0));
        assertThat(criteria.getEndTimeExclusive()).isEqualTo(LocalDateTime.of(2026, 6, 30, 0, 0));
        assertThat(criteria.getProductModels()).containsExactly("Alpha");
        assertThat(criteria.getAppVersions()).containsExactly("8.1.0");
        assertThat(criteria.getRomVersions()).isEmpty();
    }

    @Test
    void npsTrendFillsMissingDaysAndCalculatesCumulativeScores() {
        when(analyticsMapper.selectNpsTrend(any(), anyString())).thenReturn(List.of(
                npsBucket(LocalDate.of(2026, 6, 1), 2, 1, 0, 1),
                npsBucket(LocalDate.of(2026, 6, 3), 2, 2, 0, 0)));

        var response = service.queryNpsTrend(new ScoreAnalyticsRequest(
                LocalDate.of(2026, 6, 1),
                LocalDate.of(2026, 6, 3),
                null,
                null,
                null,
                null));

        assertThat(response.points()).hasSize(3);
        assertThat(response.points()).extracting("periodStartDate")
                .containsExactly(
                        LocalDate.of(2026, 6, 1),
                        LocalDate.of(2026, 6, 2),
                        LocalDate.of(2026, 6, 3));
        assertThat(response.points()).extracting("questionnaireCount")
                .containsExactly(2L, 0L, 2L);
        assertThat(response.points().get(0).npsScore()).isEqualByComparingTo("0");
        assertThat(response.points().get(1).npsScore()).isNull();
        assertThat(response.points().get(1).cumulativeNpsScore()).isEqualByComparingTo("0");
        assertThat(response.points().get(2).npsScore()).isEqualByComparingTo("100");
        assertThat(response.points().get(2).cumulativeNpsScore()).isEqualByComparingTo("50");
    }

    @Test
    void nssFeatureScoresSupportDescendingSortAndKeepMissingScoresLast() {
        when(analyticsMapper.selectNssFeatureScores(any())).thenReturn(List.of(
                nssFeature(1L, "F1", "续航", "2026-01-01T09:00:00", 1, 0, 1),
                nssFeature(2L, "F2", "影像", "2026-01-02T09:00:00", 2, 0, 0),
                nssFeature(3L, "F3", "性能", "2026-01-03T09:00:00", 0, 0, 0)));

        var response = service.queryNssFeatures(new NssFeatureAnalyticsRequest(
                LocalDate.of(2026, 6, 1),
                LocalDate.of(2026, 6, 30),
                null,
                null,
                null,
                null,
                "desc"));

        assertThat(response.features()).extracting(NssFeatureScoreResponse::featureId)
                .containsExactly(2L, 1L, 3L);
        assertThat(response.features().get(0).nssScore()).isEqualByComparingTo("100");
        assertThat(response.features().get(1).nssScore()).isEqualByComparingTo("0");
        assertThat(response.features().get(2).nssScore()).isNull();
    }

    @Test
    void filterOptionsCombineAnswerValuesAndEnabledFeatures() {
        when(analyticsMapper.selectProductOptions()).thenReturn(List.of(
                new AnalyticsProductOptionRow(10L, "P10", "Alpha")));
        when(analyticsMapper.selectAppVersionOptions()).thenReturn(List.of("8.1.0"));
        when(analyticsMapper.selectRomVersionOptions()).thenReturn(List.of("OS1.0"));
        when(featureMapper.selectEnabledFeatures()).thenReturn(List.of(feature(7L, "F7", "续航")));

        ScoreAnalyticsFilterOptionsResponse response = service.queryFilterOptions();

        assertThat(response.products()).hasSize(1);
        assertThat(response.products().get(0).productId()).isEqualTo(10L);
        assertThat(response.products().get(0).productCode()).isEqualTo("P10");
        assertThat(response.products().get(0).productModel()).isEqualTo("Alpha");
        assertThat(response.appVersions()).containsExactly("8.1.0");
        assertThat(response.romVersions()).containsExactly("OS1.0");
        assertThat(response.features()).hasSize(1);
        assertThat(response.features().get(0).featureId()).isEqualTo(7L);
        assertThat(response.features().get(0).featureCode()).isEqualTo("F7");
        assertThat(response.features().get(0).featureName()).isEqualTo("续航");
    }

    private NpsAnalyticsBucketRow npsBucket(LocalDate date,
                                            long count,
                                            long promoters,
                                            long passives,
                                            long detractors) {
        NpsAnalyticsBucketRow row = new NpsAnalyticsBucketRow();
        row.setPeriodStartDate(date);
        row.setQuestionnaireCount(count);
        row.setPromoterCount(promoters);
        row.setPassiveCount(passives);
        row.setDetractorCount(detractors);
        return row;
    }

    private NssFeatureAnalyticsRow nssFeature(Long id,
                                              String code,
                                              String name,
                                              String createdAt,
                                              long promoters,
                                              long passives,
                                              long detractors) {
        NssFeatureAnalyticsRow row = new NssFeatureAnalyticsRow();
        row.setFeatureId(id);
        row.setFeatureCode(code);
        row.setFeatureName(name);
        row.setCreatedAt(LocalDateTime.parse(createdAt));
        row.setScoreCount(promoters + passives + detractors);
        row.setPromoterCount(promoters);
        row.setPassiveCount(passives);
        row.setDetractorCount(detractors);
        return row;
    }

    private FeatureRef feature(Long id, String code, String name) {
        FeatureRef feature = new FeatureRef();
        feature.setId(id);
        feature.setFeatureCode(code);
        feature.setFeatureName(name);
        feature.setSortNo(10);
        return feature;
    }
}
