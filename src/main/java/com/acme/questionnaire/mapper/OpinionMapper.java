package com.acme.questionnaire.mapper;

import com.acme.questionnaire.param.OpinionInsertParam;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface OpinionMapper {
    int deleteByAnswerIds(@Param("answerIds") List<Long> answerIds);

    int batchInsert(@Param("list") List<OpinionInsertParam> list);
}
