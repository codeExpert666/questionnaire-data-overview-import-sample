package com.acme.questionnaire.mapper;

import com.acme.questionnaire.param.FeatureScoreInsertParam;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface FeatureScoreMapper {
    int deleteByAnswerIds(@Param("answerIds") List<Long> answerIds);

    int batchInsert(@Param("list") List<FeatureScoreInsertParam> list);
}
