package com.acme.questionnaire.dto;

/**
 * 修改 pq_product 启停状态的请求体。
 *
 * <p>停用不会删除数据库记录，也不会影响已经写入的历史答卷；它只影响新模板和新导入。</p>
 *
 * @param status 1=启用，可被新模板和新导入引用；0=停用或软删除
 */
public record ProductStatusRequest(Integer status) {
}
