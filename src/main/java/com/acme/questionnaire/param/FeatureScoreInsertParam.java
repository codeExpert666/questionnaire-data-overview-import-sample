package com.acme.questionnaire.param;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 写入 pq_answer_feature_score 的批量参数。
 *
 * <p>导入解析阶段只为 Excel 中非空且适用于该产品的特性评分创建该参数。</p>
 */
@Data
@AllArgsConstructor
public class FeatureScoreInsertParam {
    /** pq_answer.id。 */
    private Long answerId;

    /** pq_feature.id。 */
    private Long featureId;

    /** 1 到 10 的整数评分。 */
    private Integer score;
}
