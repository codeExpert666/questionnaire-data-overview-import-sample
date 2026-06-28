package com.acme.questionnaire.dto;

import java.math.BigDecimal;

/**
 * NPS 总览指标。
 *
 * @param npsScore NPS 评分，单位为百分数；分母为 0 时返回 null。
 * @param averageRecommendScore 推荐意愿平均分，最多保留一位小数。
 * @param distribution 推荐者、中立者、贬损者数量分布。
 */
public record NpsOverviewResponse(
        BigDecimal npsScore,
        BigDecimal averageRecommendScore,
        RecommendDistributionResponse distribution
) {
}
