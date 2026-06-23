package com.acme.questionnaire.dto;

import com.acme.questionnaire.model.QuestionnaireProduct;

import java.util.List;

/**
 * 某个产品的特性适用关系配置视图。
 *
 * <p>该响应用于配置页一次性展示产品主数据和全量特性字典。features 中每个选项的 selected
 * 表示当前产品在 pq_product_feature 中是否启用该关系，页面保存时应把用户最终勾选结果作为
 * 完整 featureIds 集合提交。</p>
 *
 * @param productId pq_product.id
 * @param productCode 稳定产品编码
 * @param productModel 产品型号展示名
 * @param productStatus 产品启停状态
 * @param features 全量特性选项及当前产品勾选状态，包含停用特性以便展示历史配置
 */
public record ProductFeatureConfigurationResponse(
        Long productId,
        String productCode,
        String productModel,
        Integer productStatus,
        List<ProductFeatureOptionResponse> features
) {
    /**
     * 从产品实体和特性选项组装不可变响应。
     *
     * <p>List.copyOf 用于固定返回快照，避免调用方在响应创建后继续修改 features。</p>
     */
    public static ProductFeatureConfigurationResponse from(QuestionnaireProduct product,
                                                           List<ProductFeatureOptionResponse> features) {
        return new ProductFeatureConfigurationResponse(
                product.getId(),
                product.getProductCode(),
                product.getProductModel(),
                product.getStatus(),
                List.copyOf(features));
    }
}
