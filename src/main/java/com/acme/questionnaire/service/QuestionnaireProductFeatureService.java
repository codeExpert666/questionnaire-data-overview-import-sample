package com.acme.questionnaire.service;

import com.acme.questionnaire.dto.ProductFeatureConfigurationResponse;
import com.acme.questionnaire.dto.ProductFeatureOptionResponse;
import com.acme.questionnaire.dto.ProductFeatureSaveRequest;
import com.acme.questionnaire.exception.QuestionnaireProductFeatureException;
import com.acme.questionnaire.mapper.FeatureMapper;
import com.acme.questionnaire.mapper.ProductFeatureMapper;
import com.acme.questionnaire.mapper.ProductMapper;
import com.acme.questionnaire.model.QuestionnaireFeature;
import com.acme.questionnaire.model.QuestionnaireProduct;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * pq_product_feature 产品-特性适用关系配置服务。
 *
 * <p>配置入口以产品为单位整包保存适用特性集合：调用方提交当前产品应启用的
 * featureIds，服务负责把未提交的旧关系置为停用，并把提交的关系插入或重新启用。</p>
 */
@Service
@RequiredArgsConstructor
public class QuestionnaireProductFeatureService {
    private static final String INVALID_CODE = "QUESTIONNAIRE_PRODUCT_FEATURE_INVALID";
    private static final String NOT_FOUND_CODE = "QUESTIONNAIRE_PRODUCT_FEATURE_NOT_FOUND";
    private static final int ENABLED = 1;

    private final ProductMapper productMapper;
    private final FeatureMapper featureMapper;
    private final ProductFeatureMapper productFeatureMapper;
    private final QuestionnaireCacheVersionService cacheVersionService;

    /**
     * 查询某个产品的全量特性配置视图。
     *
     * <p>返回所有特性字典项，包含停用特性；停用特性用于展示历史关系，但保存时不能作为
     * 新的启用适用关系提交。</p>
     */
    public ProductFeatureConfigurationResponse listProductFeatures(Long productId) {
        QuestionnaireProduct product = requireExistingProduct(productId);
        List<QuestionnaireFeature> features = featureMapper.selectAllFeatures();
        Set<Long> selectedFeatureIds = Set.copyOf(
                productFeatureMapper.selectEnabledFeatureIdsByProductId(productId));
        return buildResponse(product, features, selectedFeatureIds);
    }

    /**
     * 按产品整包保存当前启用的特性适用关系。
     *
     * <p>featureIds 会先去重并按当前特性排序稳定化。只有存在且 status=1 的特性可以保存为
     * 启用关系；空列表表示清空该产品全部适用特性。</p>
     */
    @Transactional(rollbackFor = Exception.class)
    public ProductFeatureConfigurationResponse saveProductFeatures(Long productId,
                                                                   ProductFeatureSaveRequest request) {
        QuestionnaireProduct product = requireExistingProduct(productId);
        List<QuestionnaireFeature> features = featureMapper.selectAllFeatures();
        List<Long> targetFeatureIds = normalizeAndValidateFeatureIds(
                request == null ? null : request.featureIds(),
                features);

        Set<Long> currentFeatureIds = Set.copyOf(
                productFeatureMapper.selectEnabledFeatureIdsByProductId(productId));
        Set<Long> targetFeatureIdSet = new LinkedHashSet<>(targetFeatureIds);
        if (!currentFeatureIds.equals(targetFeatureIdSet)) {
            productFeatureMapper.disableAllByProductId(productId);
            if (!targetFeatureIds.isEmpty()) {
                productFeatureMapper.upsertProductFeatures(productId, targetFeatureIds);
            }
            cacheVersionService.increaseAfterCommit();
        }

        return buildResponse(product, features, targetFeatureIdSet);
    }

    private QuestionnaireProduct requireExistingProduct(Long productId) {
        validateProductId(productId);
        QuestionnaireProduct product = productMapper.selectById(productId);
        if (product == null) {
            throw notFound(productId);
        }
        return product;
    }

    private void validateProductId(Long productId) {
        if (productId == null || productId <= 0) {
            throw invalid("产品ID必须为正整数");
        }
    }

    private List<Long> normalizeAndValidateFeatureIds(List<Long> featureIds,
                                                      List<QuestionnaireFeature> features) {
        if (featureIds == null) {
            throw invalid("特性ID列表不能为空");
        }

        Map<Long, QuestionnaireFeature> featureById = features.stream()
                .collect(Collectors.toMap(
                        QuestionnaireFeature::getId,
                        Function.identity(),
                        (left, right) -> left));
        LinkedHashSet<Long> normalized = new LinkedHashSet<>();
        for (Long featureId : featureIds) {
            if (featureId == null || featureId <= 0) {
                throw invalid("特性ID必须为正整数");
            }
            QuestionnaireFeature feature = featureById.get(featureId);
            if (feature == null) {
                throw invalid("特性不存在：" + featureId);
            }
            if (feature.getStatus() == null || feature.getStatus() != ENABLED) {
                throw invalid("特性已停用：" + featureId);
            }
            normalized.add(featureId);
        }

        Map<Long, Integer> sortIndexById = buildSortIndex(features);
        return normalized.stream()
                .sorted(Comparator.comparingInt(sortIndexById::get))
                .toList();
    }

    private Map<Long, Integer> buildSortIndex(List<QuestionnaireFeature> features) {
        Map<Long, Integer> sortIndexById = new java.util.HashMap<>();
        for (int index = 0; index < features.size(); index++) {
            sortIndexById.put(features.get(index).getId(), index);
        }
        return sortIndexById;
    }

    private ProductFeatureConfigurationResponse buildResponse(QuestionnaireProduct product,
                                                              List<QuestionnaireFeature> features,
                                                              Set<Long> selectedFeatureIds) {
        List<ProductFeatureOptionResponse> options = features.stream()
                .map(feature -> ProductFeatureOptionResponse.from(
                        feature,
                        selectedFeatureIds.contains(feature.getId())))
                .toList();
        return ProductFeatureConfigurationResponse.from(product, options);
    }

    private QuestionnaireProductFeatureException invalid(String message) {
        return new QuestionnaireProductFeatureException(
                INVALID_CODE,
                HttpStatus.BAD_REQUEST,
                message);
    }

    private QuestionnaireProductFeatureException notFound(Long productId) {
        return new QuestionnaireProductFeatureException(
                NOT_FOUND_CODE,
                HttpStatus.NOT_FOUND,
                "产品不存在：" + productId);
    }
}
