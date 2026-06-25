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
 * 因此停用特性不会出现在新模板中，也不会被新导入接受。产品相关查找同样只包含
 * pq_product.status=1 的产品，停用产品不会进入产品字典，也不能被新文件引用。</p>
 *
 * <p>pq_product_feature 在这里被压缩成 productId -> featureId 集合，用于导入行级校验。
 * 该集合只表达“某产品是否允许填写某特性”，不影响模板动态评分列的生成；模板列仍由
 * enabledFeatures 的全量启用特性决定。</p>
 */
public class ImportReferenceData {
    private final List<FeatureRef> enabledFeatures;
    /** 当前启用产品列表，顺序决定模板“产品字典”工作表的展示顺序。 */
    private final List<ProductRef> enabledProducts;
    private final Map<String, FeatureRef> featureByCode;
    private final Map<Long, FeatureRef> featureById;
    /** 按稳定 productCode 索引启用产品，供 Excel “产品编码”列解析。 */
    private final Map<String, ProductRef> productByCode;

    /**
     * 按产品分组的启用特性 ID 集合。
     *
     * <p>仅包含 pq_product_feature.status=1 且关联特性启用的关系；没有配置关系的产品会命中空集合，
     * 表示所有动态评分列都必须留空，观点“特性分类编码”也不能填写。</p>
     */
    private final Map<Long, Set<Long>> enabledFeatureIdsByProduct;

    /**
     * 构造导入引用数据索引。
     *
     * <p>productByCode 用于把 Excel “产品编码”解析为 pq_product.id，并配合产品型号做人工可读
     * 字段的交叉校验。featureByCode 用于解析 Excel “特性分类编码”；featureById 用于错误提示；
     * enabledFeatureIdsByProduct 用于判断某个产品是否允许填写某个特性评分或观点归类。
     * productFeatures 必须来自 ProductFeatureMapper.selectEnabledProductFeatures()，即已经过滤掉停用关系
     * 和停用特性。</p>
     */
    public ImportReferenceData(List<FeatureRef> enabledFeatures,
                               List<ProductRef> products,
                               List<ProductFeatureRef> productFeatures) {
        this.enabledFeatures = List.copyOf(enabledFeatures);
        this.enabledProducts = List.copyOf(products);
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
     * 返回当前启用产品列表。
     *
     * <p>模板产品字典和导入校验使用同一份产品快照。</p>
     */
    public List<ProductRef> getEnabledProducts() {
        return enabledProducts;
    }

    /**
     * 返回按 featureCode 索引的启用特性，用于解析固定列“特性分类编码”。
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

    /**
     * 按稳定产品编码查找启用产品。
     *
     * <p>只命中当前 status=1 的产品；停用或不存在的编码都会返回 null，并由导入监听器转换为
     * “产品编码不存在”的行级校验错误。</p>
     */
    public ProductRef findProductByCode(String productCode) {
        return productByCode.get(productCode);
    }

    /**
     * 判断产品是否支持某个启用特性。
     *
     * <p>只有 pq_product_feature.status=1 且关联 pq_feature.status=1 时才返回 true。导入监听器会用该方法
     * 同时校验动态评分列和观点“特性分类编码”；返回 false 时，非空评分或特性分类都会被拒绝。</p>
     */
    public boolean productSupportsFeature(Long productId, Long featureId) {
        return enabledFeatureIdsByProduct
                .getOrDefault(productId, Collections.emptySet())
                .contains(featureId);
    }
}
