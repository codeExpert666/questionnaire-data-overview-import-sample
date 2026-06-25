package com.acme.questionnaire.controller;

import com.acme.questionnaire.dto.FeatureCreateRequest;
import com.acme.questionnaire.dto.FeatureResponse;
import com.acme.questionnaire.dto.FeatureStatusRequest;
import com.acme.questionnaire.dto.FeatureUpdateRequest;
import com.acme.questionnaire.service.QuestionnaireFeatureService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * pq_feature 特性字典管理接口。
 *
 * <p>该接口只维护特性自身的字典属性：稳定编码、展示名称、排序号和启停状态。
 * 产品与特性的适用关系由 pq_product_feature 维护，模板下载和导入校验会基于
 * 已启用特性以及产品适用关系共同判断。</p>
 *
 * <p>删除采用软删除语义，即将 status 置为 0。历史答卷评分
 * pq_answer_feature_score 和观点归类 pq_opinion.feature_id 仍通过外键保留原特性引用。</p>
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/product-questionnaires/features")
public class QuestionnaireFeatureController {
    private final QuestionnaireFeatureService featureService;

    /**
     * 查询全部特性，包含已启用和已停用数据。
     *
     * <p>用于后台维护页展示完整字典；模板下载只会读取启用特性，不直接使用该列表。</p>
     */
    @GetMapping
    public List<FeatureResponse> listFeatures() {
        return featureService.listFeatures();
    }

    /**
     * 创建特性字典项。
     *
     * <p>featureCode 创建后不提供修改入口，作为 API 入参和 Excel 固定列“特性分类编码”的稳定标识。</p>
     */
    @PostMapping
    public FeatureResponse createFeature(@RequestBody FeatureCreateRequest request) {
        return featureService.createFeature(request);
    }

    /**
     * 修改特性展示属性。
     *
     * <p>仅允许修改 featureName 和 sortNo；变更后旧模板的动态评分列会因名称或顺序不匹配被拒绝。</p>
     */
    @PutMapping("/{id}")
    public FeatureResponse updateFeature(@PathVariable Long id,
                                         @RequestBody FeatureUpdateRequest request) {
        return featureService.updateFeature(id, request);
    }

    /**
     * 启用或停用特性。
     *
     * <p>status=1 的特性进入模板动态评分列；status=0 的特性不再出现在新模板中，
     * 也不会被新导入文件引用。</p>
     */
    @PatchMapping("/{id}/status")
    public FeatureResponse changeStatus(@PathVariable Long id,
                                        @RequestBody FeatureStatusRequest request) {
        return featureService.changeStatus(id, request);
    }

    /**
     * 软删除特性。
     *
     * <p>等价于将 status 置为 0，不物理删除 pq_feature 记录。</p>
     */
    @DeleteMapping("/{id}")
    public FeatureResponse deleteFeature(@PathVariable Long id) {
        return featureService.deleteFeature(id);
    }
}
