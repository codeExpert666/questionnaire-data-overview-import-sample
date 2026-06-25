package com.acme.questionnaire.mapper;

import com.acme.questionnaire.param.AnswerIdRow;
import com.acme.questionnaire.param.AnswerUpsertParam;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * pq_answer 问卷主表 Mapper。
 *
 * <p>Excel 导入使用 source_system + questionnaire_id 定位一份问卷快照。写入时先 upsert 主表，
 * 再回查数据库生成的 answer_id，供特性评分和观点明细执行整体覆盖。</p>
 */
public interface AnswerMapper {
    /**
     * 批量插入或更新问卷主表。
     *
     * <p>唯一键由数据库保证；重复导入相同问卷时更新问卷级字段，但不在这里处理明细表。</p>
     */
    int batchUpsert(@Param("list") List<AnswerUpsertParam> list);

    /**
     * 按来源和问卷 ID 回查主键。
     *
     * <p>写库组件依赖返回数量与输入问卷数一致；缺失主键说明 upsert 或唯一键契约异常，应整体失败。</p>
     */
    List<AnswerIdRow> selectIdsByQuestionnaireIds(@Param("sourceSystem") String sourceSystem,
                                                   @Param("questionnaireIds") List<String> questionnaireIds);
}
