package com.acme.questionnaire.dto;

/**
 * 前端表格列元数据。
 *
 * <p>评分页会先返回固定列，再按当前启用特性的模板顺序追加动态评分列。
 * 动态评分列 key 与行数据中的扁平字段保持一致，统一为 featureScore:{featureId}；
 * title 使用 Excel 模板中的特性评分表头规则。</p>
 */
public record TableColumnResponse(
        String key,
        String title,
        boolean sortable,
        boolean filterable
) {
}
