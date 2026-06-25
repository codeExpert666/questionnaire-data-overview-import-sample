package com.acme.questionnaire.param;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * pq_answer 批量 upsert 参数。
 *
 * <p>由导入写库组件从 AnswerSnapshot 转换而来，只包含问卷主表字段；评分和观点明细不在这里写入。</p>
 */
@Data
@Builder
public class AnswerUpsertParam {
    /** 固定为 EXCEL，用于和其他来源的 questionnaire_id 隔离。 */
    private String sourceSystem;
    /** Excel “问卷ID”，在同一来源内作为外部稳定键。 */
    private String questionnaireId;
    /** 由产品编码解析出的 pq_product.id。 */
    private Long productId;
    private LocalDateTime answerTime;
    private String romVersion;
    private String appVersion;
    private String feedbackText;
    private String scoreReason;
    private Integer recommendScore;
    private Integer userCategory;
}
