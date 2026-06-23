package com.acme.questionnaire.ref;

import lombok.Data;

/**
 * 模板下载和导入校验使用的启用产品引用。
 *
 * <p>该对象来自 pq_product.status=1 的快照，只包含导入流程需要的主键、稳定编码和展示型号。
 * 维护字段如 status、created_at、updated_at 不进入引用数据，避免导入流程误用后台管理状态。</p>
 */
@Data
public class ProductRef {
    /** pq_product.id，写入 pq_answer.product_id 并用于校验产品-特性适用关系。 */
    private Long id;

    /** 稳定产品编码，匹配 Excel “产品编码”列。 */
    private String productCode;

    /** 当前启用产品型号，匹配 Excel “产品型号”列并展示在模板产品字典页。 */
    private String productModel;
}
