package com.acme.questionnaire.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * NPS 趋势单个周期点。
 *
 * @param periodStartDate 周期起始日期；按周时为周一，按月时为当月 1 日。
 * @param questionnaireCount 当前周期内满足筛选条件的问卷数。
 * @param npsScore 当前周期 NPS 评分，单位为百分数；当前周期无有效样本时返回 null。
 * @param cumulativeNpsScore 从请求起始日期累计至当前周期的 NPS 评分，单位为百分数。
 */
public record NpsTrendPointResponse(
        LocalDate periodStartDate,
        long questionnaireCount,
        BigDecimal npsScore,
        BigDecimal cumulativeNpsScore
) {
}
