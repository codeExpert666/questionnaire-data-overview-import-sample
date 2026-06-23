package com.acme.questionnaire.model;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * pq_product 表对应的产品型号字典模型。
 *
 * <p>该模型只表达产品主数据本身。产品支持哪些特性由 pq_product_feature 表描述，
 * 不在本模型中冗余保存。</p>
 */
@Data
public class QuestionnaireProduct {
    /** pq_product.id，内部主键，被 pq_answer.product_id 和 pq_product_feature.product_id 引用。 */
    private Long id;

    /**
     * 稳定产品编码。
     *
     * <p>用于 Excel “产品编码”列和外部配置匹配；创建后不修改，数据库唯一索引保证不重复。</p>
     */
    private String productCode;

    /**
     * 产品型号展示名。
     *
     * <p>模板“产品字典”页展示该字段；导入时必须与产品编码对应的当前启用型号一致。</p>
     */
    private String productModel;

    /**
     * 启停状态。
     *
     * <p>1 表示启用，进入新模板和导入校验；0 表示停用或软删除，历史答卷外键仍保留。</p>
     */
    private Integer status;

    /** 数据库创建时间。 */
    private LocalDateTime createdAt;

    /** 数据库最后更新时间。 */
    private LocalDateTime updatedAt;
}
