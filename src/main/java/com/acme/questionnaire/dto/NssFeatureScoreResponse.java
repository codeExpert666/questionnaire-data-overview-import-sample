package com.acme.questionnaire.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 单个启用特性的 NSS 得分。
 *
 * @param featureId 特性 ID。
 * @param featureCode 稳定特性编码。
 * @param featureName 特性展示名称。
 * @param createdAt 特性创建时间，用于柱状图维度信息展示。
 * @param nssScore 该特性 NSS 评分，单位为百分数；无有效评分样本时返回 null。
 */
public record NssFeatureScoreResponse(
        Long featureId,
        String featureCode,
        String featureName,
        LocalDateTime createdAt,
        BigDecimal nssScore
) {
}
