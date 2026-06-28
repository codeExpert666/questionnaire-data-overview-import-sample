package com.acme.questionnaire.dto;

import java.util.List;

/**
 * 单特性 NSS 趋势响应。
 *
 * @param featureId 特性 ID。
 * @param featureCode 稳定特性编码。
 * @param featureName 特性展示名称。
 * @param points 按周期起始日期升序排列的趋势点，包含无数据周期的补点。
 */
public record NssTrendResponse(
        Long featureId,
        String featureCode,
        String featureName,
        List<NssTrendPointResponse> points
) {
}
