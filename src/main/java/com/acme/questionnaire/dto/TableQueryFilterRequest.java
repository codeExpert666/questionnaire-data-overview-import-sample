package com.acme.questionnaire.dto;

import java.time.LocalDateTime;

/**
 * 展示页固定字段过滤条件。
 *
 * <p>该 record 被概览、评分和观点查询复用。评分页会读取问卷 ID、产品编码、产品型号、
 * 答卷时间、ROM/App 版本、推荐意愿评分、用户归类和 featureId；sentiment、featureCategoryName
 * 和 keyword 属于观点/概览文本查询能力，评分页会显式拒绝，避免调用方误以为评分表存在观点粒度过滤。</p>
 *
 * <p>字符串字段在服务层会 trim，空字符串按未传处理；时间范围为闭区间；评分范围必须落在
 * 1 到 10 且最小值不能大于最大值。</p>
 *
 * @param questionnaireId 问卷 ID 模糊匹配条件，对应导入 Excel 中的“问卷ID”。
 * @param productCode 产品编码模糊匹配条件，对应 pq_product.product_code。
 * @param productModel 产品型号模糊匹配条件，对应 pq_product.product_model。
 * @param answerTimeStart 答卷时间闭区间开始值，非空时匹配 answer_time &gt;= 该值。
 * @param answerTimeEnd 答卷时间闭区间结束值，非空时匹配 answer_time &lt;= 该值。
 * @param romVersion ROM 版本模糊匹配条件。
 * @param appVersion App 版本模糊匹配条件。
 * @param recommendScoreMin 推荐意愿评分下限，非空时必须在 1 到 10。
 * @param recommendScoreMax 推荐意愿评分上限，非空时必须在 1 到 10。
 * @param userCategory 用户归类枚举编码，必须是 UserCategory 支持的编码。
 * @param sentiment 情感观点枚举编码；仅观点/概览查询支持，评分查询会拒绝。
 * @param featureId 特性主键；仅评分查询支持，表示要求该问卷存在该特性评分。
 * @param featureCategoryName 特性分类名称模糊匹配条件；仅观点/概览查询支持。
 * @param keyword 文本关键词；用于反馈文本、打分原因和观点内容模糊匹配，评分查询会拒绝。
 */
public record TableQueryFilterRequest(
        String questionnaireId,
        String productCode,
        String productModel,
        LocalDateTime answerTimeStart,
        LocalDateTime answerTimeEnd,
        String romVersion,
        String appVersion,
        Integer recommendScoreMin,
        Integer recommendScoreMax,
        Integer userCategory,
        Integer sentiment,
        Long featureId,
        String featureCategoryName,
        String keyword
) {
}
