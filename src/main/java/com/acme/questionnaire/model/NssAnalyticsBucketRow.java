package com.acme.questionnaire.model;

import lombok.Data;

import java.time.LocalDate;

/**
 * 单特性 NSS 趋势周期原始聚合行。
 */
@Data
public class NssAnalyticsBucketRow {
    /** 周期起始日期，由受控粒度表达式计算得到。 */
    private LocalDate periodStartDate;

    /** 当前周期内该特性的有效评分样本数。 */
    private Long questionnaireCount;

    /** 当前周期该特性评分 9 到 10 的样本数。 */
    private Long promoterCount;

    /** 当前周期该特性评分 7 到 8 的样本数。 */
    private Long passiveCount;

    /** 当前周期该特性评分 1 到 6 的样本数。 */
    private Long detractorCount;
}
