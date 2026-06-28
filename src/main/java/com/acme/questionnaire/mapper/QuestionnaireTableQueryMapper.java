package com.acme.questionnaire.mapper;

import com.acme.questionnaire.model.DataOverviewQueryRow;
import com.acme.questionnaire.model.FeatureScoreCell;
import com.acme.questionnaire.model.FeatureScoreSortClause;
import com.acme.questionnaire.model.OpinionQueryRow;
import com.acme.questionnaire.model.ScoreQueryRow;
import com.acme.questionnaire.model.TableOrderClause;
import com.acme.questionnaire.model.TableQueryCriteria;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface QuestionnaireTableQueryMapper {
    long countDataOverview(@Param("criteria") TableQueryCriteria criteria);

    List<DataOverviewQueryRow> selectDataOverviewRows(
            @Param("criteria") TableQueryCriteria criteria,
            @Param("orderClauses") List<TableOrderClause> orderClauses,
            @Param("featureScoreSorts") List<FeatureScoreSortClause> featureScoreSorts,
            @Param("offset") int offset,
            @Param("limit") int limit);

    long countScores(@Param("criteria") TableQueryCriteria criteria);

    List<ScoreQueryRow> selectScoreRows(
            @Param("criteria") TableQueryCriteria criteria,
            @Param("orderClauses") List<TableOrderClause> orderClauses,
            @Param("featureScoreSorts") List<FeatureScoreSortClause> featureScoreSorts,
            @Param("offset") int offset,
            @Param("limit") int limit);

    long countOpinions(@Param("criteria") TableQueryCriteria criteria);

    List<OpinionQueryRow> selectOpinionRows(
            @Param("criteria") TableQueryCriteria criteria,
            @Param("orderClauses") List<TableOrderClause> orderClauses,
            @Param("offset") int offset,
            @Param("limit") int limit);

    List<FeatureScoreCell> selectFeatureScoresByAnswerIds(
            @Param("answerIds") List<Long> answerIds);
}
