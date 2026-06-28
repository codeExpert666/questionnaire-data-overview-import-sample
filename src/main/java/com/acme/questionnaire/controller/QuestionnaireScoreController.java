package com.acme.questionnaire.controller;

import com.acme.questionnaire.dto.ScoreRowResponse;
import com.acme.questionnaire.dto.TablePageResponse;
import com.acme.questionnaire.dto.TableQueryRequest;
import com.acme.questionnaire.service.QuestionnaireTableQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 问卷评分页查询接口。
 *
 * <p>评分页的展示粒度是一份问卷一行，固定字段来自 pq_answer 和 pq_product，
 * 动态特性评分列来自 pq_answer_feature_score。该控制器只维护 HTTP 路由和
 * JSON 契约，分页、过滤、排序白名单、动态列装配等规则统一下沉到
 * QuestionnaireTableQueryService。</p>
 *
 * <p>接口使用 POST + JSON，是因为评分页支持复杂过滤条件和
 * featureScore:{featureId} 形式的动态评分列排序，使用请求体比 GET 查询串更稳定。</p>
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/product-questionnaires/scores")
public class QuestionnaireScoreController {
    private final QuestionnaireTableQueryService queryService;

    /**
     * 查询评分页分页数据。
     *
     * <p>请求体可为空；为空时服务层使用默认分页和默认排序。TableQueryRequest 中的
     * filters 支持问卷、产品、答卷时间、ROM/App 版本、推荐意愿评分、用户归类和特性 ID；
     * featureScoreFilters 支持按具体特性评分区间过滤；sorts 支持固定字段和
     * featureScore:{featureId} 动态评分字段。</p>
     *
     * <p>评分页不展示观点文本，因此不会接受 sentiment 和 keyword 过滤。非法过滤项、
     * 非法评分区间、未启用特性 ID、非白名单排序字段都会由服务层抛出
     * QuestionnaireQueryException，并交给全局异常处理器转换为 400 响应。</p>
     *
     * <p>响应中的 columns 描述前端表格列；rows 中每条 ScoreRowResponse 是一份问卷的
     * 得分快照，动态评分会通过 JsonAnyGetter 扁平化为 featureScore:{featureId} 字段。</p>
     */
    @PostMapping("/query")
    public TablePageResponse<ScoreRowResponse> queryScores(
            @RequestBody(required = false) TableQueryRequest request) {
        return queryService.queryScores(request);
    }
}
