package com.acme.questionnaire.model;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 评分页基础行查询结果。
 *
 * <p>该对象只承载 pq_answer + pq_product 的一问卷一行基础字段，不直接包含动态特性评分。
 * 动态评分由 QuestionnaireTableQueryService 在分页后按 answerId 批量查询并补到
 * ScoreRowResponse，避免主查询因 pq_answer_feature_score 一对多关系放大行数。</p>
 */
@Data
public class ScoreQueryRow {
    private Long answerId;
    private String questionnaireId;
    private String productModel;
    private String productCode;
    private LocalDateTime answerTime;
    private String romVersion;
    private String appVersion;
    private Integer recommendScore;
    private Integer userCategory;
}
