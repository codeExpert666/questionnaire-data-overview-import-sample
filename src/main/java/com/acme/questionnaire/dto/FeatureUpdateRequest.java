package com.acme.questionnaire.dto;

/**
 * 更新 pq_feature 可变展示属性的请求体。
 *
 * <p>不包含 featureCode，避免修改稳定编码导致旧模板和历史导入数据失去可解析标识。</p>
 *
 * @param featureName 新展示名称，导入旧模板时会触发名称变化提示
 * @param sortNo 新排序号，用于模板动态评分列顺序
 */
public record FeatureUpdateRequest(
        String featureName,
        Integer sortNo
) {
}
