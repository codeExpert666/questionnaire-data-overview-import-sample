package com.acme.questionnaire.service;

import com.acme.questionnaire.mapper.FeatureMapper;
import com.acme.questionnaire.mapper.ProductFeatureMapper;
import com.acme.questionnaire.mapper.ProductMapper;
import com.acme.questionnaire.ref.ImportReferenceData;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 导入和模板下载的引用数据加载器。
 *
 * <p>每次下载模板或导入文件时重新读取当前数据库状态，确保 pq_feature 的启停、名称和排序变更
 * 能立即反映到模板表头和导入校验中。</p>
 */
@Component
@RequiredArgsConstructor
public class ImportReferenceDataLoader {
    private final FeatureMapper featureMapper;
    private final ProductMapper productMapper;
    private final ProductFeatureMapper productFeatureMapper;

    /**
     * 加载一次处理所需的产品、启用特性和产品特性关系快照。
     */
    public ImportReferenceData load() {
        return new ImportReferenceData(
                featureMapper.selectEnabledFeatures(),
                productMapper.selectEnabledProducts(),
                productFeatureMapper.selectEnabledProductFeatures());
    }
}
