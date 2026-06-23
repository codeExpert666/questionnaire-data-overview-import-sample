package com.acme.questionnaire.dto;

/**
 * 修改 pq_product 启停状态的请求体。
 *
 * @param status 1=启用，可被新模板和新导入引用；0=停用
 */
public record ProductStatusRequest(Integer status) {
}
