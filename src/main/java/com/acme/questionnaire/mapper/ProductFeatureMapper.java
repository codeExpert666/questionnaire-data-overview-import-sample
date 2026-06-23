package com.acme.questionnaire.mapper;

import com.acme.questionnaire.ref.ProductFeatureRef;

import java.util.List;

public interface ProductFeatureMapper {
    List<ProductFeatureRef> selectEnabledProductFeatures();
}
