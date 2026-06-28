package com.acme.questionnaire.dto;

import java.util.List;

/**
 * 展示页统一分页响应。
 *
 * <p>columns 描述本次响应的表格列，rows 承载当前页数据。评分页 rows 中的动态评分字段会与
 * columns 的 featureScore:{featureId} key 对齐，前端不需要提前知道当前启用特性集合。</p>
 *
 * @param columns 本次查询返回的列元数据，包含固定列和当前启用特性对应的动态列。
 * @param pageNo 当前页码，从 1 开始；由服务层按请求值或默认值规范化后返回。
 * @param pageSize 当前每页条数；由服务层按请求值、默认值和最大上限规范化后返回。
 * @param total 满足过滤条件的总行数，不受当前页 offset/limit 限制。
 * @param rows 当前页数据行；具体元素类型由概览、评分或观点查询接口决定。
 * @param <T> 当前页行响应类型。
 */
public record TablePageResponse<T>(
        List<TableColumnResponse> columns,
        int pageNo,
        int pageSize,
        long total,
        List<T> rows
) {
}
