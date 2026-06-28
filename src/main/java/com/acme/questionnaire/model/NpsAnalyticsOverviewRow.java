package com.acme.questionnaire.model;

import lombok.Data;

import java.math.BigDecimal;

/**
 * NPS 总览原始聚合行。
 */
@Data
public class NpsAnalyticsOverviewRow {
    /** 当前筛选范围内的问卷数。 */
    private Long questionnaireCount;

    /** 推荐意愿评分 9 到 10 的问卷数。 */
    private Long promoterCount;

    /** 推荐意愿评分 7 到 8 的问卷数。 */
    private Long passiveCount;

    /** 推荐意愿评分 1 到 6 的问卷数。 */
    private Long detractorCount;

    /** 推荐意愿评分平均值。 */
    private BigDecimal averageRecommendScore;
}
