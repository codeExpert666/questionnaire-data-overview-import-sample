package com.acme.questionnaire.dto;

import com.acme.questionnaire.model.QuestionnaireProduct;

import java.util.List;

/**
 * 某个产品的特性适用关系配置视图。
 *
 * @param productId pq_product.id
 * @param productCode 稳定产品编码
 * @param productModel 产品型号展示名
 * @param productStatus 产品启停状态
 * @param features 全量特性选项及当前产品勾选状态
 */
public record ProductFeatureConfigurationResponse(
        Long productId,
        String productCode,
        String productModel,
        Integer productStatus,
        List<ProductFeatureOptionResponse> features
) {
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
