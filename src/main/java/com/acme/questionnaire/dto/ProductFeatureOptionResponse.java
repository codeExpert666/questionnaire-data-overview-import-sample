package com.acme.questionnaire.dto;

import com.acme.questionnaire.model.QuestionnaireFeature;

/**
 * 产品特性配置页中的单个特性选项。
 *
 * <p>该对象来自启用的 pq_feature 字典，并附加当前产品在 pq_product_feature 中的 selected 状态。
 * 软删除特性不进入配置表单，保存新的适用关系时也只能提交 status=1 的特性。</p>
 *
 * @param id pq_feature.id
 * @param featureCode 稳定特性编码
 * @param featureName 特性展示名称
 * @param sortNo 模板列排序号
 * @param status 特性启停状态，1=启用，0=停用
 * @param selected 当前产品是否启用该特性适用关系，即 pq_product_feature.status=1
 */
public record ProductFeatureOptionResponse(
        Long id,
        String featureCode,
        String featureName,
        Integer sortNo,
        Integer status,
        boolean selected
) {
    /**
     * 把特性字典项和产品侧勾选状态合并为配置选项。
     */
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
