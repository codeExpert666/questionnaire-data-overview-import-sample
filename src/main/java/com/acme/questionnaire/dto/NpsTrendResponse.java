package com.acme.questionnaire.dto;

import java.util.List;

/**
 * NPS 趋势响应。
 *
 * @param points 按周期起始日期升序排列的趋势点，包含无数据周期的补点。
 */
public record NpsTrendResponse(
        List<NpsTrendPointResponse> points
) {
}
