package com.acme.questionnaire.dto;

/**
 * 动态特性评分区间过滤条件。
 *
 * <p>评分页和数据概览页可以按启用特性维度过滤评分，featureId 必须是当前启用的
 * pq_feature.id。min 和 max 均可单独传入，表示单边区间条件；两者同时为空时表示只要求
 * 该问卷存在此特性的评分记录。观点页不支持该过滤，因为观点查询的粒度是 pq_opinion。</p>
 *
 * @param featureId 当前启用特性主键。
 * @param min 最低评分，允许为空；非空时必须在 1 到 10。
 * @param max 最高评分，允许为空；非空时必须在 1 到 10。
 */
public record FeatureScoreFilterRequest(
        Long featureId,
        Integer min,
        Integer max
) {
}
