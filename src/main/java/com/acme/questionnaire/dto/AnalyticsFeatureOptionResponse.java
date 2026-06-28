package com.acme.questionnaire.dto;

/**
 * 评分分析筛选中的启用特性选项。
 *
 * @param featureId pq_feature.id，用于 NSS 趋势切换和前端选项值。
 * @param featureCode 稳定特性编码，便于前端或外部系统识别。
 * @param featureName 特性展示名称，供筛选框、雷达图和柱状图展示。
 */
public record AnalyticsFeatureOptionResponse(
        Long featureId,
        String featureCode,
        String featureName
) {
}
