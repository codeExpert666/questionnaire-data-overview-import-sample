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
 * featureIds，服务负责把未提交的旧关系置为停用，并把提交的关系插入或重新启用。
 * 该关系是导入校验的产品侧白名单：动态评分列和观点“特性分类名称”都必须命中启用关系。</p>
 *
 * <p>服务不会修改 pq_feature 字典，也不会影响模板中的动态列集合。模板列集合只由启用特性决定；
 * pq_product_feature 只在具体产品导入时决定某一列是否允许填值。</p>
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
     * 查询某个产品的启用特性配置视图。
     *
     * <p>只返回启用特性字典项，避免已软删除特性以 selected=true 出现在表单中并被前端误提交。
     * selected 表示当前产品与该启用特性之间存在启用关系。</p>
     */
    public ProductFeatureConfigurationResponse listProductFeatures(Long productId) {
        QuestionnaireProduct product = requireEnabledProduct(productId);
        List<QuestionnaireFeature> features = enabledFeatures(featureMapper.selectAllFeatures());
        Set<Long> selectedFeatureIds = Set.copyOf(
                productFeatureMapper.selectEnabledFeatureIdsByProductId(productId));
        return buildResponse(product, features, selectedFeatureIds);
    }

    /**
     * 按产品整包保存当前启用的特性适用关系。
     *
     * <p>featureIds 会先去重并按当前特性排序稳定化。只有存在且 status=1 的特性可以保存为
     * 启用关系；空列表表示清空该产品全部适用特性。保存时如果集合没有变化，不写库也不递增缓存版本。</p>
     *
     * <p>发生变化时先停用旧集合，再批量 upsert 新集合。这样可以保留历史关系行的创建时间和审计线索，
     * 同时让恢复某个旧关系只需要把 status 重新置为 1。</p>
     */
    @Transactional(rollbackFor = Exception.class)
    public ProductFeatureConfigurationResponse saveProductFeatures(Long productId,
                                                                   ProductFeatureSaveRequest request) {
        QuestionnaireProduct product = requireEnabledProduct(productId);
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

        return buildResponse(product, enabledFeatures(features), targetFeatureIdSet);
    }

    private QuestionnaireProduct requireExistingProduct(Long productId) {
        validateProductId(productId);
        QuestionnaireProduct product = productMapper.selectById(productId);
        if (product == null) {
            throw notFound(productId);
        }
        return product;
    }

    private QuestionnaireProduct requireEnabledProduct(Long productId) {
        QuestionnaireProduct product = requireExistingProduct(productId);
        if (product.getStatus() == null || product.getStatus() != ENABLED) {
            throw invalid("产品已停用：" + productId);
        }
        return product;
    }

    private void validateProductId(Long productId) {
        if (productId == null || productId <= 0) {
            throw invalid("产品ID必须为正整数");
        }
    }

    /**
     * 规范化并校验提交的特性 ID 集合。
     *
     * <p>前端可能按用户勾选顺序或重复提交 ID；这里统一去重，并按 featureMapper.selectAllFeatures()
     * 的字典顺序排序，保证 Mapper 收到的列表稳定。所有 ID 必须存在且对应 pq_feature.status=1，
     * 防止把已停用特性重新配置为可导入。</p>
     */
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

    private List<QuestionnaireFeature> enabledFeatures(List<QuestionnaireFeature> features) {
        return features.stream()
                .filter(feature -> feature.getStatus() != null && feature.getStatus() == ENABLED)
                .toList();
    }

    /**
     * 组装配置页响应。
     *
     * <p>响应保持启用特性顺序不变，通过 selected 标记当前产品的启用关系。软删除特性不进入
     * 配置表单，避免保存时被误提交。</p>
     */
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
