package com.acme.questionnaire.dto;

import com.acme.questionnaire.model.QuestionnaireFeature;

import java.time.LocalDateTime;

/**
 * pq_feature 维护接口返回值。
 *
 * @param id 内部主键
 * @param featureCode 稳定编码
 * @param featureName 展示名称
 * @param sortNo 模板列排序号
 * @param status 启停状态，1=启用，0=停用
 * @param createdAt 创建时间
 * @param updatedAt 更新时间
 */
public record FeatureResponse(
        Long id,
        String featureCode,
        String featureName,
        Integer sortNo,
        Integer status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    /**
     * 将数据库模型转换为接口响应。
     */
    public static FeatureResponse from(QuestionnaireFeature feature) {
        return new FeatureResponse(
                feature.getId(),
                feature.getFeatureCode(),
                feature.getFeatureName(),
                feature.getSortNo(),
                feature.getStatus(),
                feature.getCreatedAt(),
                feature.getUpdatedAt());
    }
}
