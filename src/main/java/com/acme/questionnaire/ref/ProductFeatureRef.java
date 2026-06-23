package com.acme.questionnaire.ref;

import lombok.Data;

/**
 * pq_product_feature 的启用关系引用。
 *
 * <p>该引用只用于模板导入/下载阶段的只读快照，不用于配置页展示历史关系。Mapper 已经过滤出
 * pq_product_feature.status=1 且 pq_feature.status=1 的记录，因此这里的每一条关系都表示
 * “当前产品可接受该特性的评分和观点归类”。</p>
 */
@Data
public class ProductFeatureRef {
    /** pq_product.id；导入时由产品编码解析得到，用于匹配产品侧适用关系。 */
    private Long productId;

    /** pq_feature.id；必须是当前启用特性，写入评分和观点时使用同一主键。 */
    private Long featureId;
}
