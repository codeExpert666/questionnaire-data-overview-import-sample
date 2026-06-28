package com.acme.questionnaire.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 评分分析筛选中的产品选项查询行。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AnalyticsProductOptionRow {
    /** pq_product.id，保留为选项标识。 */
    private Long productId;

    /** pq_product.product_code，产品稳定编码。 */
    private String productCode;

    /** pq_product.product_model，analytics 产品型号筛选值。 */
    private String productModel;
}
