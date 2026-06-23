package com.acme.questionnaire.dto;

import com.acme.questionnaire.model.QuestionnaireFeature;

/**
 * 产品特性配置页中的单个特性选项。
 *
 * @param id pq_feature.id
 * @param featureCode 稳定特性编码
 * @param featureName 特性展示名称
 * @param sortNo 模板列排序号
 * @param status 特性启停状态，1=启用，0=停用
 * @param selected 当前产品是否启用该特性适用关系
 */
public record ProductFeatureOptionResponse(
        Long id,
        String featureCode,
        String featureName,
        Integer sortNo,
        Integer status,
        boolean selected
) {
    public static ProductFeatureOptionResponse from(QuestionnaireFeature feature,
                                                    boolean selected) {
        return new ProductFeatureOptionResponse(
                feature.getId(),
                feature.getFeatureCode(),
                feature.getFeatureName(),
                feature.getSortNo(),
                feature.getStatus(),
                selected);
    }
}
