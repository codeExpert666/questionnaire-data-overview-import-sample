package com.acme.questionnaire.dto;

/**
 * 评分分析筛选中的产品选项。
 *
 * @param productId pq_product.id，仅用于标识选项来源。
 * @param productCode 稳定产品编码，随产品型号一起返回便于前端展示或排查。
 * @param productModel 产品型号展示名，也是 analytics 请求中的产品多选值。
 */
public record AnalyticsProductOptionResponse(
        Long productId,
        String productCode,
        String productModel
) {
}
