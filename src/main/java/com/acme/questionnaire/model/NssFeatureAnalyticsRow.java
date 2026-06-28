package com.acme.questionnaire.model;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 单个启用特性的 NSS 原始聚合行。
 */
@Data
public class NssFeatureAnalyticsRow {
    /** pq_feature.id。 */
    private Long featureId;

    /** pq_feature.feature_code。 */
    private String featureCode;

    /** pq_feature.feature_name。 */
    private String featureName;

    /** 特性创建时间，用于柱状图维度信息展示。 */
    private LocalDateTime createdAt;

    /** 当前筛选范围内该特性的有效评分样本数。 */
    private Long scoreCount;

    /** 该特性评分 9 到 10 的样本数。 */
    private Long promoterCount;

    /** 该特性评分 7 到 8 的样本数。 */
    private Long passiveCount;

    /** 该特性评分 1 到 6 的样本数。 */
    private Long detractorCount;
}
