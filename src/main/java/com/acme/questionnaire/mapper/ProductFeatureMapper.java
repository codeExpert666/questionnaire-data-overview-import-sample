package com.acme.questionnaire.mapper;

import com.acme.questionnaire.ref.ProductFeatureRef;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * pq_product_feature 产品-特性适用关系 Mapper。
 *
 * <p>该表是产品维度的特性白名单。模板下载不按产品裁剪动态评分列；导入某个产品的数据时，
 * 通过这里读取的启用关系判断评分列或观点分类是否允许写入。</p>
 */
public interface ProductFeatureMapper {
    /**
     * 查询导入校验使用的启用产品-特性关系快照。
     *
     * <p>该查询同时要求 pq_product_feature.status=1 和 pq_feature.status=1。关系启用但特性已停用时，
     * 新导入不应再接受该特性评分或观点分类，因此不会返回。</p>
     */
    List<ProductFeatureRef> selectEnabledProductFeatures();

    /**
     * 查询某个产品当前启用的特性适用关系。
     *
     * <p>配置页使用该结果判断 selected 状态；该查询同时要求 pq_feature.status=1，避免已软删除
     * 特性继续以选中状态进入配置表单。</p>
     */
    List<Long> selectEnabledFeatureIdsByProductId(Long productId);

    /**
     * 停用某个产品下全部当前启用关系。
     *
     * <p>整包保存前调用，只把 status 置为 0，不删除行；历史关系保留后，重新勾选同一特性时可以
     * 通过 upsert 恢复为启用。</p>
     */
    int disableAllByProductId(Long productId);

    /**
     * 插入或重新启用某个产品下的一组特性关系。
     *
     * <p>productId 与 featureId 使用联合主键，重复关系通过 ON DUPLICATE KEY UPDATE 恢复 status=1，
     * 并刷新 updated_at。</p>
     */
    int upsertProductFeatures(@Param("productId") Long productId,
                              @Param("featureIds") List<Long> featureIds);
}
