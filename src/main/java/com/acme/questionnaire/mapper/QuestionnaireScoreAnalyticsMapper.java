package com.acme.questionnaire.mapper;

import com.acme.questionnaire.model.AnalyticsProductOptionRow;
import com.acme.questionnaire.model.NpsAnalyticsBucketRow;
import com.acme.questionnaire.model.NpsAnalyticsOverviewRow;
import com.acme.questionnaire.model.NssAnalyticsBucketRow;
import com.acme.questionnaire.model.NssFeatureAnalyticsRow;
import com.acme.questionnaire.model.ScoreAnalyticsCriteria;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 评分 Tab 可视化分析 Mapper。
 *
 * <p>Mapper 只返回聚合计数和筛选选项；NPS/NSS 百分数、累计口径、空周期补点和排序规则
 * 统一由 QuestionnaireScoreAnalyticsService 处理。</p>
 */
public interface QuestionnaireScoreAnalyticsMapper {
    /**
     * 查询已产生答卷数据的产品型号筛选选项。
     */
    List<AnalyticsProductOptionRow> selectProductOptions();

    /**
     * 查询已产生答卷数据且非空的 App 版本选项。
     */
    List<String> selectAppVersionOptions();

    /**
     * 查询已产生答卷数据且非空的 ROM 版本选项。
     */
    List<String> selectRomVersionOptions();

    /**
     * 按公共筛选条件聚合 NPS 总览所需计数和推荐意愿平均分。
     */
    NpsAnalyticsOverviewRow selectNpsOverview(@Param("criteria") ScoreAnalyticsCriteria criteria);

    /**
     * 按受控周期表达式聚合 NPS 趋势原始计数。
     */
    List<NpsAnalyticsBucketRow> selectNpsTrend(
            @Param("criteria") ScoreAnalyticsCriteria criteria,
            @Param("bucketExpression") String bucketExpression);

    /**
     * 查询所有启用特性在当前筛选范围内的 NSS 原始计数。
     */
    List<NssFeatureAnalyticsRow> selectNssFeatureScores(
            @Param("criteria") ScoreAnalyticsCriteria criteria);

    /**
     * 查询指定启用特性的 NSS 趋势原始计数。
     */
    List<NssAnalyticsBucketRow> selectNssTrend(
            @Param("criteria") ScoreAnalyticsCriteria criteria,
            @Param("featureId") Long featureId,
            @Param("bucketExpression") String bucketExpression);
}
