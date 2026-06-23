package com.acme.questionnaire.dto;

import com.acme.questionnaire.model.QuestionnaireProduct;

import java.time.LocalDateTime;

/**
 * pq_product 维护接口返回值。
 *
 * @param id 内部主键
 * @param productCode 稳定产品编码
 * @param productModel 产品型号展示名
 * @param status 启停状态，1=启用，0=停用
 * @param createdAt 创建时间
 * @param updatedAt 更新时间
 */
public record ProductResponse(
        Long id,
        String productCode,
        String productModel,
        Integer status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    /**
     * 将数据库模型转换为接口响应。
     */
    public static ProductResponse from(QuestionnaireProduct product) {
        return new ProductResponse(
                product.getId(),
                product.getProductCode(),
                product.getProductModel(),
                product.getStatus(),
                product.getCreatedAt(),
                product.getUpdatedAt());
    }
}
