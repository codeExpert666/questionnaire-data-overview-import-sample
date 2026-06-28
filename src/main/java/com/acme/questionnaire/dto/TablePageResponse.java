package com.acme.questionnaire.dto;

import java.util.List;

/**
 * 展示页统一分页响应。
 *
 * <p>columns 描述本次响应的表格列，rows 承载当前页数据。评分页 rows 中的动态评分字段会与
 * columns 的 featureScore:{featureId} key 对齐，前端不需要提前知道当前启用特性集合。</p>
 */
public record TablePageResponse<T>(
        List<TableColumnResponse> columns,
        int pageNo,
        int pageSize,
        long total,
        List<T> rows
) {
}
