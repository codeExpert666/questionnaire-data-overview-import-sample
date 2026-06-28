package com.acme.questionnaire.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 单特性 NSS 趋势周期点。
 *
 * @param periodStartDate 周期起始日期；按周时为周一，按月时为当月 1 日。
 * @param questionnaireCount 当前周期内该特性的有效评分样本数。
 * @param nssScore 当前周期该特性的 NSS 评分，单位为百分数；当前周期无样本时返回 null。
 * @param cumulativeNssScore 从请求起始日期累计至当前周期的 NSS 评分，单位为百分数。
 */
public record NssTrendPointResponse(
        LocalDate periodStartDate,
        long questionnaireCount,
        BigDecimal nssScore,
        BigDecimal cumulativeNssScore
) {
}
