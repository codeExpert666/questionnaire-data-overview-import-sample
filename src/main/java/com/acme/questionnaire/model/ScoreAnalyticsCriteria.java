package com.acme.questionnaire.model;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 评分分析 Mapper 使用的标准化筛选条件。
 */
@Value
@Builder
public class ScoreAnalyticsCriteria {
    /** 请求数据周期开始日期，供服务层补点和累计口径使用。 */
    LocalDate startDate;

    /** 请求数据周期结束日期，供服务层补点和累计口径使用。 */
    LocalDate endDate;

    /** 答卷时间过滤下界，闭区间。 */
    LocalDateTime startTimeInclusive;

    /** 答卷时间过滤上界，开区间，等于结束日期次日零点。 */
    LocalDateTime endTimeExclusive;

    /** 产品型号精确多选条件；空集合表示不过滤。 */
    List<String> productModels;

    /** App 版本精确多选条件；空集合表示不过滤。 */
    List<String> appVersions;

    /** ROM 版本精确多选条件；空集合表示不过滤。 */
    List<String> romVersions;
}
