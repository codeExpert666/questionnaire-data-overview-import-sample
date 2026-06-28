package com.acme.questionnaire.model;

import lombok.Value;

/**
 * 动态特性评分排序 join 描述。
 *
 * <p>服务层在解析 featureScore:{featureId} 排序字段后生成该对象。alias 由服务层按序号生成，
 * featureId 已校验为当前启用特性，因此 XML 中的动态 join 只消费受控别名和受控特性 ID。</p>
 */
@Value
public class FeatureScoreSortClause {
    /** 排序 join 使用的表别名，例如 sort_score_0。 */
    String alias;
    /** 需要排序的 pq_feature.id。 */
    Long featureId;
}
