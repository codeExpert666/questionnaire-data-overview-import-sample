package com.acme.questionnaire.controller;

import com.acme.questionnaire.dto.NpsOverviewResponse;
import com.acme.questionnaire.dto.NpsTrendResponse;
import com.acme.questionnaire.dto.NssFeatureAnalyticsRequest;
import com.acme.questionnaire.dto.NssFeatureScoresResponse;
import com.acme.questionnaire.dto.NssTrendAnalyticsRequest;
import com.acme.questionnaire.dto.NssTrendResponse;
import com.acme.questionnaire.dto.ScoreAnalyticsFilterOptionsResponse;
import com.acme.questionnaire.dto.ScoreAnalyticsRequest;
import com.acme.questionnaire.service.QuestionnaireScoreAnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 评分 Tab 可视化分析接口。
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/product-questionnaires/scores/analytics")
public class QuestionnaireScoreAnalyticsController {
    private final QuestionnaireScoreAnalyticsService analyticsService;

    /**
     * 查询评分分析公共筛选选项。
     *
     * <p>这些选项只服务 analytics 图表区域，与评分表格页的筛选条件互不影响。</p>
     */
    @GetMapping("/filter-options")
    public ScoreAnalyticsFilterOptionsResponse filterOptions() {
        return analyticsService.queryFilterOptions();
    }

    /**
     * 查询 NPS 总览指标和推荐意愿三类人群数量分布。
     */
    @PostMapping("/nps/overview")
    public NpsOverviewResponse npsOverview(@RequestBody(required = false) ScoreAnalyticsRequest request) {
        return analyticsService.queryNpsOverview(request);
    }

    /**
     * 查询 NPS 趋势。
     *
     * <p>响应包含周期问卷数、周期 NPS 和从筛选起始日期累计至当前周期的 NPS。</p>
     */
    @PostMapping("/nps/trend")
    public NpsTrendResponse npsTrend(@RequestBody(required = false) ScoreAnalyticsRequest request) {
        return analyticsService.queryNpsTrend(request);
    }

    /**
     * 查询所有启用特性的 NSS 得分。
     *
     * <p>用于 NSS 雷达图和各维度柱状图；可按评分升序或降序排序。</p>
     */
    @PostMapping("/nss/features")
    public NssFeatureScoresResponse nssFeatures(
            @RequestBody(required = false) NssFeatureAnalyticsRequest request) {
        return analyticsService.queryNssFeatures(request);
    }

    /**
     * 查询指定启用特性的 NSS 趋势。
     *
     * <p>响应包含周期有效评分样本数、周期 NSS 和累计 NSS。</p>
     */
    @PostMapping("/nss/trend")
    public NssTrendResponse nssTrend(@RequestBody(required = false) NssTrendAnalyticsRequest request) {
        return analyticsService.queryNssTrend(request);
    }
}
