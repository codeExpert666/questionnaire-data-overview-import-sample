package com.acme.questionnaire.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * 评分分析图表的公共筛选请求。
 *
 * @param startDate 数据周期开始日期，按答卷时间过滤，空值默认近三月起始日。
 * @param endDate 数据周期结束日期，按答卷时间过滤，空值默认当天。
 * @param productModels 产品型号多选；空集合表示全部产品型号。
 * @param appVersions 客户端 App 版本多选；空集合表示全部版本。
 * @param romVersions ROM 版本多选；空集合表示全部版本。
 * @param granularity 数据粒度，空值默认按天。
 */
public record ScoreAnalyticsRequest(
        LocalDate startDate,
        LocalDate endDate,
        List<String> productModels,
        List<String> appVersions,
        List<String> romVersions,
        ScoreAnalyticsGranularity granularity
) {
}
