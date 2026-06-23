package com.acme.questionnaire.dto;

/**
 * 创建 pq_product 的请求体。
 *
 * @param productCode 稳定产品编码，创建后不提供修改入口
 * @param productModel 产品型号展示名
 * @param status 启停状态，1=启用，0=停用；为空时默认启用
 */
public record ProductCreateRequest(
        String productCode,
        String productModel,
        Integer status
) {
}
