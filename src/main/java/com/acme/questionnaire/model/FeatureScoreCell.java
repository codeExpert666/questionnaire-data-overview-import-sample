package com.acme.questionnaire.model;

import lombok.Data;

/**
 * 特性评分单元格查询结果。
 *
 * <p>评分页和数据概览页在拿到分页 answerId 后，用该投影批量读取动态评分明细，
 * 再按 answerId + featureId 写回响应行。</p>
 */
@Data
public class FeatureScoreCell {
    /** pq_answer.id，用于匹配分页结果中的行。 */
    private Long answerId;
    /** pq_feature.id，对应响应动态字段 featureScore:{featureId}。 */
    private Long featureId;
    /** 该问卷在该特性上的评分。 */
    private Integer score;
}
