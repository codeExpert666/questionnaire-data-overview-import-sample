package com.acme.questionnaire.dto;

/**
 * 更新 pq_product 可变展示属性的请求体。
 *
 * <p>更新接口只开放 productModel。productCode 是导入稳定匹配键，不通过更新请求体暴露。</p>
 *
 * @param productModel 产品型号展示名，去除首尾空白后不能为空，长度不超过 128
 */
public record ProductUpdateRequest(String productModel) {
}
