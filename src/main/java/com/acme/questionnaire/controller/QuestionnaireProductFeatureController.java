package com.acme.questionnaire.controller;

import com.acme.questionnaire.dto.ProductFeatureConfigurationResponse;
import com.acme.questionnaire.dto.ProductFeatureSaveRequest;
import com.acme.questionnaire.service.QuestionnaireProductFeatureService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * pq_product_feature 产品-特性适用关系配置接口。
 *
 * <p>该接口只维护“产品是否支持某个特性”的配置关系，不维护特性字典本身，也不裁剪模板列。
 * 模板下载仍展示全部启用 pq_feature；导入时再通过 pq_product_feature.status=1 判断当前产品
 * 是否允许填写对应的动态特性评分列或固定列“特性分类编码”。</p>
 *
 * <p>保存接口采用整包语义：请求体中的 featureIds 是保存后的完整启用集合，而不是增量补丁。
 * 未提交的旧关系会被置为停用，提交的关系会插入或重新启用。</p>
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/product-questionnaires/products/{productId}/features")
public class QuestionnaireProductFeatureController {
    private final QuestionnaireProductFeatureService productFeatureService;

    /**
     * 查询某个产品的特性配置视图。
     *
     * <p>返回全量特性及 selected 状态，便于配置页面直接渲染复选项。响应中会保留停用特性，
     * 用于让页面识别历史上已配置但当前不允许继续选择的特性。</p>
     */
    @GetMapping
    public ProductFeatureConfigurationResponse listProductFeatures(@PathVariable Long productId) {
        return productFeatureService.listProductFeatures(productId);
    }

    /**
     * 整包保存某个产品的启用特性集合。
     *
     * <p>请求体中的 featureIds 表示保存后的完整集合，未提交的旧关系会被停用。保存成功后，
     * 后续模板导入会立即按新关系校验产品适用性。</p>
     */
    @PutMapping
    public ProductFeatureConfigurationResponse saveProductFeatures(
            @PathVariable Long productId,
            @RequestBody ProductFeatureSaveRequest request) {
        return productFeatureService.saveProductFeatures(productId, request);
    }
}
