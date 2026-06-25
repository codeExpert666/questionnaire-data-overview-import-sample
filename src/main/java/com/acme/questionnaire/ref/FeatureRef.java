package com.acme.questionnaire.ref;

import lombok.Data;

/**
 * 导入和模板场景中的启用特性引用。
 *
 * <p>只承载生成模板与校验导入所需字段，避免把后台维护模型中的 status、时间戳等字段泄漏到
 * Excel 处理流程。</p>
 */
@Data
public class FeatureRef {
    /** pq_feature.id，写入评分和观点时使用。 */
    private Long id;

    /** pq_feature.feature_code，后台和 API 使用的稳定特性编码。 */
    private String featureCode;

    /** pq_feature.feature_name，Excel 评分列表头展示和旧模板检测使用。 */
    private String featureName;

    /** pq_feature.sort_no，决定模板动态列顺序。 */
    private Integer sortNo;
}
