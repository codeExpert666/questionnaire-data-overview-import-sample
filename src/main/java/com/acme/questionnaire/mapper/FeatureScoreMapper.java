package com.acme.questionnaire.mapper;

import com.acme.questionnaire.param.FeatureScoreInsertParam;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * pq_answer_feature_score 特性评分 Mapper。
 *
 * <p>评分记录使用 answer_id + feature_id 作为主键。导入覆盖同一问卷时，调用方会先删除旧评分，
 * 再批量插入本次 Excel 中的非空评分。</p>
 */
public interface FeatureScoreMapper {
    /**
     * 删除答卷下的全部特性评分。
     */
    int deleteByAnswerIds(@Param("answerIds") List<Long> answerIds);

    /**
     * 批量插入非空特性评分。
     *
     * <p>每个 item.featureId 必须来自当前启用 pq_feature，且已通过产品适用性校验。</p>
     */
    int batchInsert(@Param("list") List<FeatureScoreInsertParam> list);
}
