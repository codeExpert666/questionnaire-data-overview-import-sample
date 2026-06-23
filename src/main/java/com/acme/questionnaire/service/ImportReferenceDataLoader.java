package com.acme.questionnaire.service;

import com.acme.questionnaire.mapper.FeatureMapper;
import com.acme.questionnaire.mapper.ProductFeatureMapper;
import com.acme.questionnaire.mapper.ProductMapper;
import com.acme.questionnaire.ref.ImportReferenceData;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ImportReferenceDataLoader {
    private final FeatureMapper featureMapper;
    private final ProductMapper productMapper;
    private final ProductFeatureMapper productFeatureMapper;

    public ImportReferenceData load() {
        return new ImportReferenceData(
                featureMapper.selectEnabledFeatures(),
                productMapper.selectAllProducts(),
                productFeatureMapper.selectEnabledProductFeatures());
    }
}
