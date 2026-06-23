package com.acme.questionnaire.dto;

/**
 * 更新 pq_product 可变展示属性的请求体。
 *
 * @param productModel 产品型号展示名
 */
public record ProductUpdateRequest(String productModel) {
}
