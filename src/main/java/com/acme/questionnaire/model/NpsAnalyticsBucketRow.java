package com.acme.questionnaire.model;

import lombok.Data;

import java.time.LocalDate;

/**
 * NPS 趋势周期原始聚合行。
 */
@Data
public class NpsAnalyticsBucketRow {
    /** 周期起始日期，由受控粒度表达式计算得到。 */
    private LocalDate periodStartDate;

    /** 当前周期问卷数。 */
    private Long questionnaireCount;

    /** 当前周期推荐意愿评分 9 到 10 的问卷数。 */
    private Long promoterCount;

    /** 当前周期推荐意愿评分 7 到 8 的问卷数。 */
    private Long passiveCount;

    /** 当前周期推荐意愿评分 1 到 6 的问卷数。 */
    private Long detractorCount;
}
