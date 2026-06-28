package com.acme.questionnaire.dto;

/**
 * 前端表格列元数据。
 *
 * <p>评分页会先返回固定列，再按当前启用特性的模板顺序追加动态评分列。
 * 动态评分列 key 与行数据中的扁平字段保持一致，统一为 featureScore:{featureId}；
 * title 使用 Excel 模板中的特性评分表头规则。</p>
 *
 * @param key 前端读取行数据时使用的字段 key；动态评分列统一为 featureScore:{featureId}。
 * @param title 前端展示的列标题；动态评分列复用 Excel 模板评分表头规则。
 * @param sortable 是否允许前端对该列发起排序请求；最终仍由服务层排序白名单校验。
 * @param filterable 是否允许前端对该列展示过滤入口；最终仍由服务层过滤规则校验。
 */
public record TableColumnResponse(
        String key,
        String title,
        boolean sortable,
        boolean filterable
) {
}
