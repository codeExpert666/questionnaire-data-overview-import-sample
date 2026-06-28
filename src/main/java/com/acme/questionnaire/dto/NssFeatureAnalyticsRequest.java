package com.acme.questionnaire.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * NSS 多特性得分请求。
 *
 * <p>该请求服务于 NSS 雷达图和各维度柱状图。它复用评分分析公共筛选字段；
 * granularity 作为公共筛选字段保留，但本接口按整个数据周期聚合，不按粒度分桶。</p>
 *
 * @param startDate 数据周期开始日期，空值默认近三月起始日。
 * @param endDate 数据周期结束日期，空值默认当天。
 * @param productModels 产品型号多选；空集合表示全部产品型号。
 * @param appVersions 客户端 App 版本多选；空集合表示全部版本。
 * @param romVersions ROM 版本多选；空集合表示全部版本。
 * @param granularity 公共筛选字段，本接口不按该字段分桶。
 * @param sortDirection 评分排序方向，支持 asc/desc；空值保持启用特性顺序。
 */
public record NssFeatureAnalyticsRequest(
        LocalDate startDate,
        LocalDate endDate,
        List<String> productModels,
        List<String> appVersions,
        List<String> romVersions,
        ScoreAnalyticsGranularity granularity,
        String sortDirection
) {
}
