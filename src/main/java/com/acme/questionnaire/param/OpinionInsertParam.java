package com.acme.questionnaire.param;

import lombok.Builder;
import lombok.Data;

/**
 * pq_opinion 批量插入参数。
 *
 * <p>每条参数对应 Excel 中一行观点明细，写入前已经完成情感枚举和特性分类适用性校验。</p>
 */
@Data
@Builder
public class OpinionInsertParam {
    /** pq_answer.id。 */
    private Long answerId;
    /** 同一答卷内的观点顺序，从 1 开始。 */
    private Integer opinionSeq;
    /** 情感观点枚举编码。 */
    private Integer sentimentCode;
    /** 可为空；为空表示该观点未归类到具体特性。 */
    private Long featureId;
    private String feedbackContent1;
    private String feedbackContent2;
}
