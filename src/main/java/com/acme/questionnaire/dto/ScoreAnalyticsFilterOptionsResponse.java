package com.acme.questionnaire.dto;

import java.util.List;

/**
 * 评分分析公共筛选选项。
 *
 * <p>该响应只给 analytics 公共筛选区使用，不影响评分表格查询的筛选状态。</p>
 *
 * @param products 已产生答卷数据的产品型号选项。
 * @param appVersions 已产生答卷数据且非空的 App 版本选项。
 * @param romVersions 已产生答卷数据且非空的 ROM 版本选项。
 * @param features 当前启用特性选项，用于 NSS 雷达、柱状图和趋势切换。
 */
public record ScoreAnalyticsFilterOptionsResponse(
        List<AnalyticsProductOptionResponse> products,
        List<String> appVersions,
        List<String> romVersions,
        List<AnalyticsFeatureOptionResponse> features
) {
}
