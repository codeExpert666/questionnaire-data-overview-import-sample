package com.acme.questionnaire.dto;

/**
 * 创建 pq_feature 的请求体。
 *
 * @param featureCode 稳定编码，创建后不提供修改入口；允许字母、数字、下划线、点和短横线
 * @param featureName 展示名称，会出现在 Excel 模板动态评分列表头
 * @param sortNo 模板列和维护列表排序号；为空时使用 0
 * @param status 启停状态，1=启用，0=停用；为空时默认启用
 */
public record FeatureCreateRequest(
        String featureCode,
        String featureName,
        Integer sortNo,
        Integer status
) {
}
