package com.acme.questionnaire.mapper;

import com.acme.questionnaire.ref.ProductFeatureRef;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ProductFeatureMapper {
    List<ProductFeatureRef> selectEnabledProductFeatures();

    /**
     * 查询某个产品当前启用的特性适用关系。
     *
     * <p>配置页使用该结果判断 selected 状态；不关联 pq_feature.status，便于展示历史上
     * 已配置但后来停用的特性关系。</p>
     */
    List<Long> selectEnabledFeatureIdsByProductId(Long productId);

    /**
     * 停用某个产品下全部当前启用关系。
     */
    int disableAllByProductId(Long productId);

    /**
     * 插入或重新启用某个产品下的一组特性关系。
     */
    int upsertProductFeatures(@Param("productId") Long productId,
                              @Param("featureIds") List<Long> featureIds);
}
