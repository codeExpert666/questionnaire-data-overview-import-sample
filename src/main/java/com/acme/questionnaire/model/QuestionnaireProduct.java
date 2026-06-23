package com.acme.questionnaire.model;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * pq_product 表对应的产品型号字典模型。
 */
@Data
public class QuestionnaireProduct {
    /** pq_product.id，内部主键，被答卷表作为外键引用。 */
    private Long id;

    /** 稳定产品编码；创建后不修改，用于 Excel 导入匹配。 */
    private String productCode;

    /** 产品型号展示名；导入时必须与产品编码匹配。 */
    private String productModel;

    /** 启停状态：1 表示启用，0 表示停用或软删除。 */
    private Integer status;

    /** 数据库创建时间。 */
    private LocalDateTime createdAt;

    /** 数据库最后更新时间。 */
    private LocalDateTime updatedAt;
}
