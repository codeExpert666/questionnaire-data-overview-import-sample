package com.acme.questionnaire.ref;

import lombok.Data;

/**
 * pq_product_feature 的启用关系引用。
 *
 * <p>模板会列出所有启用特性，但导入某一产品的数据时必须通过该关系确认产品是否支持特性。</p>
 */
@Data
public class ProductFeatureRef {
    /** pq_product.id。 */
    private Long productId;

    /** pq_feature.id。 */
    private Long featureId;
}
