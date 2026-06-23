package com.acme.questionnaire.ref;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class ImportReferenceData {
    private final List<FeatureRef> enabledFeatures;
    private final Map<String, FeatureRef> featureByCode;
    private final Map<Long, FeatureRef> featureById;
    private final Map<String, ProductRef> productByCode;
    private final Map<Long, Set<Long>> enabledFeatureIdsByProduct;

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

    public List<FeatureRef> getEnabledFeatures() {
        return enabledFeatures;
    }

    public Map<String, FeatureRef> getFeatureByCode() {
        return Collections.unmodifiableMap(featureByCode);
    }

    public FeatureRef findFeatureByCode(String featureCode) {
        return featureByCode.get(featureCode);
    }

    public FeatureRef findFeatureById(Long featureId) {
        return featureById.get(featureId);
    }

    public ProductRef findProductByCode(String productCode) {
        return productByCode.get(productCode);
    }

    public boolean productSupportsFeature(Long productId, Long featureId) {
        return enabledFeatureIdsByProduct
                .getOrDefault(productId, Collections.emptySet())
                .contains(featureId);
    }
}
