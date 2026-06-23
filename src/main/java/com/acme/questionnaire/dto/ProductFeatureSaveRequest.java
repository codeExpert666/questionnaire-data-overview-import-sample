package com.acme.questionnaire.dto;

import java.util.List;

/**
 * 按产品整包保存适用特性集合的请求体。
 *
 * @param featureIds 当前产品应启用的 pq_feature.id 集合；空列表表示清空全部适用关系
 */
public record ProductFeatureSaveRequest(
        List<Long> featureIds
) {
}
