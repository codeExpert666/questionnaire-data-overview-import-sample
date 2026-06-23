package com.acme.questionnaire.mapper;

import com.acme.questionnaire.param.AnswerIdRow;
import com.acme.questionnaire.param.AnswerUpsertParam;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface AnswerMapper {
    int batchUpsert(@Param("list") List<AnswerUpsertParam> list);

    List<AnswerIdRow> selectIdsByQuestionnaireIds(@Param("sourceSystem") String sourceSystem,
                                                   @Param("questionnaireIds") List<String> questionnaireIds);
}
