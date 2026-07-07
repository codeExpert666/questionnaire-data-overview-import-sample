package com.acme.questionnaire.mapper;

import com.acme.questionnaire.param.OpinionInsertParam;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * pq_opinion 观点明细 Mapper。
 *
 * <p>观点明细是问卷导入的子表快照。再次导入同一 questionnaire_id 时，调用方会先按 answer_id
 * 删除旧观点，再按 Excel 中的连续行顺序重新插入。</p>
 */
public interface OpinionMapper {
    /**
     * 删除答卷下的全部观点明细。
     */
    int deleteByAnswerIds(@Param("answerIds") List<Long> answerIds);

    /**
     * 批量插入本次导入解析出的观点明细。
     *
     * <p>每条记录的 sentimentCode 已经完成枚举校验，featureCategoryName 是用户填写的自由分类文本。</p>
     */
    int batchInsert(@Param("list") List<OpinionInsertParam> list);
}
