package com.acme.questionnaire.ref;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Excel 导入时使用的只读引用数据快照。
 *
 * <p>该对象在一次下载模板或一次导入开始时加载，保证同一次处理过程内使用同一份
 * pq_feature、pq_product 和 pq_product_feature 视图。特性相关查找只包含启用特性，
 * 因此停用特性不会出现在新模板中，也不会被新导入接受。</p>
 */
public class ImportReferenceData {
    private final List<FeatureRef> enabledFeatures;
    private final Map<String, FeatureRef> featureByCode;
    private final Map<Long, FeatureRef> featureById;
    private final Map<String, ProductRef> productByCode;
    private final Map<Long, Set<Long>> enabledFeatureIdsByProduct;

    /**
     * 构造导入引用数据索引。
     *
     * <p>featureByCode 用于解析 Excel 表头和“特性分类编码”；featureById 用于错误提示；
     * enabledFeatureIdsByProduct 用于判断某个产品是否允许填写某个特性评分或观点归类。</p>
     */
    public ImportReferenceData(List<FeatureRef> enabledFeatures,
                               List<ProductRef> products,
                               List<ProductFeatureRef> productFeatures) {
        this.enabledFeatures = List.copyOf(enabledFeatures);
        this.featureByCode = enabledFeatures.stream().collect(Collectors.toMap(
                FeatureRef::getFeatureCode,
                item -> item,
                (left, right) -> left,
                LinkedHashMap::new));
        this.featureById = enabledFeatures.stream().collect(Collectors.toMap(
                FeatureRef::getId,
                item -> item));
        this.productByCode = products.stream().collect(Collectors.toMap(
                ProductRef::getProductCode,
                item -> item,
                (left, right) -> left));
        this.enabledFeatureIdsByProduct = productFeatures.stream().collect(Collectors.groupingBy(
                ProductFeatureRef::getProductId,
                Collectors.mapping(ProductFeatureRef::getFeatureId, Collectors.toSet())));
    }

    /**
     * 返回当前启用特性列表。
     *
     * <p>列表顺序来自 Mapper，决定模板动态评分列顺序。</p>
     */
    public List<FeatureRef> getEnabledFeatures() {
        return enabledFeatures;
    }

    /**
     * 返回按 featureCode 索引的启用特性。
     */
    public Map<String, FeatureRef> getFeatureByCode() {
        return Collections.unmodifiableMap(featureByCode);
    }

    /**
     * 按稳定编码查找启用特性。
     */
    public FeatureRef findFeatureByCode(String featureCode) {
        return featureByCode.get(featureCode);
    }

    /**
     * 按主键查找启用特性。
     *
     * <p>主要用于将特性评分冲突错误转换为可读列名。</p>
     */
    public FeatureRef findFeatureById(Long featureId) {
        return featureById.get(featureId);
    }

    public ProductRef findProductByCode(String productCode) {
        return productByCode.get(productCode);
    }

    /**
     * 判断产品是否支持某个启用特性。
     *
     * <p>只有 pq_product_feature.status=1 且关联 pq_feature.status=1 时才返回 true。</p>
     */
    public boolean productSupportsFeature(Long productId, Long featureId) {
        return enabledFeatureIdsByProduct
                .getOrDefault(productId, Collections.emptySet())
                .contains(featureId);
    }
}
