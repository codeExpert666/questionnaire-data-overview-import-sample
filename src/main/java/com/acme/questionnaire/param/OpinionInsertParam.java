package com.acme.questionnaire.param;

import lombok.Builder;
import lombok.Data;

/**
 * pq_opinion 批量插入参数。
 *
 * <p>每条参数对应 Excel 中一行观点明细，写入前已经完成情感枚举校验。特性分类名称是自由文本，
 * 只做长度和空白规范化，不再绑定 pq_feature。</p>
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
    /** 可为空；保存 Excel 固定列“特性分类名称”的自由文本。 */
    private String featureCategoryName;
    private String feedbackContent1;
    private String feedbackContent2;
}
