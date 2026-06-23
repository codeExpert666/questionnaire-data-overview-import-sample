package com.acme.questionnaire.dto;

import java.util.List;

/**
 * 按产品整包保存适用特性集合的请求体。
 *
 * <p>featureIds 是保存后的完整启用集合，不是增量新增或删除列表。服务层会去重、按特性字典顺序
 * 稳定化，并拒绝不存在或已停用的特性 ID。</p>
 *
 * @param featureIds 当前产品应启用的 pq_feature.id 集合；空列表表示清空全部适用关系，null 会被拒绝
 */
public record ProductFeatureSaveRequest(
        List<Long> featureIds
) {
}
