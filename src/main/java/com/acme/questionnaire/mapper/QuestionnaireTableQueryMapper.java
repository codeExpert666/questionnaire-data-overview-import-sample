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

/**
 * 问卷展示页查询 Mapper。
 *
 * <p>评分页链路中，countScores/selectScoreRows 以 pq_answer 为分页粒度；
 * selectFeatureScoresByAnswerIds 在分页后批量读取动态评分明细。排序表达式和动态评分排序
 * join 别名都必须来自 QuestionnaireTableQueryService 的白名单和格式化结果，禁止调用方
 * 绕过服务层直接传入任意 SQL 片段。</p>
 */
public interface QuestionnaireTableQueryMapper {
    long countDataOverview(@Param("criteria") TableQueryCriteria criteria);

    List<DataOverviewQueryRow> selectDataOverviewRows(
            @Param("criteria") TableQueryCriteria criteria,
            @Param("orderClauses") List<TableOrderClause> orderClauses,
            @Param("featureScoreSorts") List<FeatureScoreSortClause> featureScoreSorts,
            @Param("offset") int offset,
            @Param("limit") int limit);

    /**
     * 统计评分页满足条件的问卷数。
     *
     * <p>统计基表是 pq_answer，因此一个 questionnaireId 只计为一行；特性评分条件通过 EXISTS
     * 过滤，不会因为一份问卷有多条评分明细而重复计数。</p>
     */
    long countScores(@Param("criteria") TableQueryCriteria criteria);

    /**
     * 查询评分页当前页基础行。
     *
     * <p>返回字段只包含 pq_answer + pq_product 的固定字段；动态评分列不在主查询中展开，
     * 由 selectFeatureScoresByAnswerIds 在服务层分页后回填。</p>
     */
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

    /**
     * 按当前页 answerId 批量读取动态特性评分。
     *
     * <p>该方法服务于评分页和概览页的响应装配，只读取明细值，不参与分页总数计算。</p>
     */
    List<FeatureScoreCell> selectFeatureScoresByAnswerIds(
            @Param("answerIds") List<Long> answerIds);
}
