package com.acme.questionnaire.dto;

/**
 * 创建 pq_product 的请求体。
 *
 * <p>服务层会统一去除文本首尾空白并校验长度。productCode 为空时自动生成 P{id}；
 * 创建后不可修改，因此调用方若自行传入，应使用能长期稳定出现在 Excel、配置和外部系统中的编码。</p>
 *
 * @param productCode 稳定产品编码；为空时自动生成 P{id}；非空时仅支持字母、数字、下划线、点和短横线，长度不超过 64
 * @param productModel 产品型号展示名，去除首尾空白后不能为空，长度不超过 128
 * @param status 启停状态，1=启用，0=停用；为空时默认启用
 */
public record ProductCreateRequest(
        String productCode,
        String productModel,
        Integer status
) {
}
