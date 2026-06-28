package com.acme.questionnaire.model;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Mapper 层消费的标准化查询条件。
 *
 * <p>外部请求不会直接进入 XML；Controller DTO 先由 QuestionnaireTableQueryService 完成
 * trim、默认值、枚举、评分范围、特性启用状态和页面级能力校验，再构造成该对象。评分页只会
 * 使用与 pq_answer、pq_product 和 pq_answer_feature_score 有关的字段。</p>
 */
@Value
@Builder
public class TableQueryCriteria {
    /** 问卷 ID 模糊匹配条件。 */
    String questionnaireId;
    /** 产品编码模糊匹配条件。 */
    String productCode;
    /** 产品型号模糊匹配条件。 */
    String productModel;
    /** 答卷时间闭区间开始。 */
    LocalDateTime answerTimeStart;
    /** 答卷时间闭区间结束。 */
    LocalDateTime answerTimeEnd;
    /** ROM 版本模糊匹配条件。 */
    String romVersion;
    /** App 版本模糊匹配条件。 */
    String appVersion;
    /** 推荐意愿评分下限。 */
    Integer recommendScoreMin;
    /** 推荐意愿评分上限。 */
    Integer recommendScoreMax;
    /** 用户归类枚举编码。 */
    Integer userCategory;
    /** 情感观点枚举编码；评分页不允许使用。 */
    Integer sentiment;
    /** 特性主键；评分页表示要求该问卷存在该特性评分。 */
    Long featureId;
    /** 文本关键词；评分页不允许使用。 */
    String keyword;
    /** 动态特性评分过滤条件，XML 中按 EXISTS 逐项约束。 */
    List<FeatureScoreFilterCriteria> featureScoreFilters;
}
