package com.acme.questionnaire.dto;

import java.time.LocalDateTime;

/**
 * 展示页固定字段过滤条件。
 *
 * <p>该 record 被概览、评分和观点查询复用。评分页会读取问卷 ID、产品编码、产品型号、
 * 答卷时间、ROM/App 版本、推荐意愿评分、用户归类和 featureId；sentiment 和 keyword
 * 属于观点/概览文本查询能力，评分页会显式拒绝，避免调用方误以为评分表存在观点粒度过滤。</p>
 *
 * <p>字符串字段在服务层会 trim，空字符串按未传处理；时间范围为闭区间；评分范围必须落在
 * 1 到 10 且最小值不能大于最大值。</p>
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
        String keyword
) {
}
