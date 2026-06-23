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
 * 能立即反映到模板表头和导入校验中。pq_product 的启停与型号变更也通过同一快照生效：
 * 启用产品进入模板“产品字典”，停用产品不能被新导入引用。</p>
 *
 * <p>产品特性关系只加载“可用于导入校验”的启用白名单，不加载配置页完整历史关系。这样可以保证
 * 停用特性或停用关系不会被新文件继续写入。</p>
 */
@Component
@RequiredArgsConstructor
public class ImportReferenceDataLoader {
    private final FeatureMapper featureMapper;
    private final ProductMapper productMapper;
    private final ProductFeatureMapper productFeatureMapper;

    /**
     * 加载一次处理所需的产品、启用特性和产品特性关系快照。
     *
     * <p>三个查询必须来自同一个业务时点：产品决定答卷归属，特性决定模板列，产品特性关系
     * 决定某产品是否允许填写某个特性评分或观点分类。调用方应在一次模板生成或一次文件导入中复用
     * 返回对象，避免同一文件处理过程中引用数据前后不一致。</p>
     */
    public ImportReferenceData load() {
        return new ImportReferenceData(
                featureMapper.selectEnabledFeatures(),
                productMapper.selectEnabledProducts(),
                productFeatureMapper.selectEnabledProductFeatures());
    }
}
