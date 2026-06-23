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
 * <p>该接口按产品整包维护适用特性集合。保存后，模板下载和导入校验会通过
 * pq_product_feature.status=1 判断某个产品是否允许填写对应特性评分或观点归类。</p>
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/product-questionnaires/products/{productId}/features")
public class QuestionnaireProductFeatureController {
    private final QuestionnaireProductFeatureService productFeatureService;

    /**
     * 查询某个产品的特性配置视图。
     *
     * <p>返回全量特性及 selected 状态，便于配置页面直接渲染复选项。</p>
     */
    @GetMapping
    public ProductFeatureConfigurationResponse listProductFeatures(@PathVariable Long productId) {
        return productFeatureService.listProductFeatures(productId);
    }

    /**
     * 整包保存某个产品的启用特性集合。
     *
     * <p>请求体中的 featureIds 表示保存后的完整集合，未提交的旧关系会被停用。</p>
     */
    @PutMapping
    public ProductFeatureConfigurationResponse saveProductFeatures(
            @PathVariable Long productId,
            @RequestBody ProductFeatureSaveRequest request) {
        return productFeatureService.saveProductFeatures(productId, request);
    }
}
