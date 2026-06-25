package com.acme.questionnaire.param;

import lombok.Data;

/**
 * pq_answer 主键回查结果。
 *
 * <p>导入覆盖子表前需要把外部 questionnaire_id 转换为内部 answer_id。</p>
 */
@Data
public class AnswerIdRow {
    /** pq_answer.id。 */
    private Long id;
    /** Excel “问卷ID”，用于回填到导入批次的聚合对象。 */
    private String questionnaireId;
}
