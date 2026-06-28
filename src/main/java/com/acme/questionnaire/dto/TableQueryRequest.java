package com.acme.questionnaire.dto;

import java.util.List;

/**
 * 三个数据展示页共用的分页查询请求。
 *
 * <p>评分页调用时，pageNo/pageSize 允许为空并由服务层兜底；filters 承载固定字段过滤；
 * featureScoreFilters 承载动态特性评分区间；sorts 承载多列排序。不同展示页支持的过滤和
 * 排序字段并不完全相同，最终以 QuestionnaireTableQueryService 的 QueryType 校验为准。</p>
 *
 * @param pageNo 页码，从 1 开始；为空、0 或负数时使用默认页码。
 * @param pageSize 每页条数；为空、0 或负数时使用默认条数，超过上限时由服务层截断。
 * @param filters 固定字段过滤条件，评分页只接受与问卷和评分相关的字段。
 * @param featureScoreFilters 动态评分列过滤，字段粒度为 pq_feature.id。
 * @param sorts 多列排序，固定字段使用响应字段名，动态评分列使用 featureScore:{featureId}。
 */
public record TableQueryRequest(
        Integer pageNo,
        Integer pageSize,
        TableQueryFilterRequest filters,
        List<FeatureScoreFilterRequest> featureScoreFilters,
        List<TableSortRequest> sorts
) {
}
