package com.acme.questionnaire.dto;

/**
 * 修改 pq_feature 启停状态的请求体。
 *
 * @param status 1=启用，进入新模板和导入校验；0=停用，不再被新模板和新导入引用
 */
public record FeatureStatusRequest(Integer status) {
}
