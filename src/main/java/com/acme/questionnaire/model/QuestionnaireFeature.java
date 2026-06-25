package com.acme.questionnaire.model;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * pq_feature 表对应的特性字典模型。
 *
 * <p>一条记录定义一个可在问卷导入模板中展示和评分的产品特性。featureCode 是对外稳定编码，
 * id 是内部外键；Excel 评分列按当前启用特性的顺序和 featureName 解析后写入 featureId。</p>
 */
@Data
public class QuestionnaireFeature {
    /** pq_feature.id，内部主键，被评分表和观点表作为外键引用。 */
    private Long id;

    /** 稳定编码；创建后不修改，用于 API 入参和后台识别。 */
    private String featureCode;

    /** 展示名称；会出现在模板评分列表头和特性字典页，导入时用于检测旧模板。 */
    private String featureName;

    /** 全量启用特性在模板中的排序号；相同排序号按 id 升序稳定排列。 */
    private Integer sortNo;

    /** 启停状态：1 表示启用，0 表示停用或软删除。 */
    private Integer status;

    /** 数据库创建时间。 */
    private LocalDateTime createdAt;

    /** 数据库最后更新时间。 */
    private LocalDateTime updatedAt;
}
