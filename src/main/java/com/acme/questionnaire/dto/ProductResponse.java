package com.acme.questionnaire.dto;

import com.acme.questionnaire.model.QuestionnaireProduct;
import com.acme.questionnaire.ref.ProductRef;

import java.time.LocalDateTime;

/**
 * pq_product 产品接口返回值。
 *
 * <p>维护列表会返回完整产品状态；启用产品筛选接口基于 ProductRef 组装响应，固定返回
 * status=1，时间字段为空。</p>
 *
 * @param id 内部主键，对应 pq_product.id
 * @param productCode 稳定产品编码，创建后不允许修改
 * @param productModel 产品型号展示名
 * @param status 启停状态，1=启用，0=停用或软删除
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

    /**
     * 将启用产品引用转换为接口响应。
     */
    public static ProductResponse from(ProductRef product) {
        return new ProductResponse(
                product.getId(),
                product.getProductCode(),
                product.getProductModel(),
                1,
                null,
                null);
    }
}
