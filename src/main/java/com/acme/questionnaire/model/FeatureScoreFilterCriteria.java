package com.acme.questionnaire.model;

import lombok.Value;

/**
 * 动态特性评分过滤的内部条件。
 *
 * <p>由 FeatureScoreFilterRequest 校验后转换得到，featureId 已确认属于当前启用特性。
 * Mapper XML 会为每个条件生成一个 EXISTS 子查询，确保同一份问卷存在该特性的评分并满足
 * min/max 区间。</p>
 */
@Value
public class FeatureScoreFilterCriteria {
    /** pq_feature.id。 */
    Long featureId;
    /** 最低评分，允许为空。 */
    Integer min;
    /** 最高评分，允许为空。 */
    Integer max;
}
