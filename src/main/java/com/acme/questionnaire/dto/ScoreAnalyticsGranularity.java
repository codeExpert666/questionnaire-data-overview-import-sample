package com.acme.questionnaire.dto;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;

/**
 * 评分分析图表的数据粒度。
 *
 * <p>SQL 分组表达式只由该枚举生成，前端传入值不会直接进入 Mapper XML。</p>
 */
public enum ScoreAnalyticsGranularity {
    DAY("a.answer_date"),
    WEEK("DATE_SUB(a.answer_date, INTERVAL WEEKDAY(a.answer_date) DAY)"),
    MONTH("DATE_SUB(a.answer_date, INTERVAL (DAYOFMONTH(a.answer_date) - 1) DAY)");

    private final String bucketExpression;

    ScoreAnalyticsGranularity(String bucketExpression) {
        this.bucketExpression = bucketExpression;
    }

    public String bucketExpression() {
        return bucketExpression;
    }

    /**
     * 计算给定日期所在周期的起始日期。
     *
     * <p>按周时使用周一作为周期起点；按月时使用当月 1 日作为周期起点。</p>
     */
    public LocalDate bucketStart(LocalDate date) {
        return switch (this) {
            case DAY -> date;
            case WEEK -> date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            case MONTH -> date.withDayOfMonth(1);
        };
    }

    /**
     * 计算下一个趋势周期的起始日期，用于服务层补齐空周期。
     */
    public LocalDate nextBucket(LocalDate date) {
        return switch (this) {
            case DAY -> date.plusDays(1);
            case WEEK -> date.plusWeeks(1);
            case MONTH -> date.plusMonths(1);
        };
    }
}
