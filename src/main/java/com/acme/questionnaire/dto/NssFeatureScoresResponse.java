package com.acme.questionnaire.dto;

import java.util.List;

/**
 * NSS 多特性得分响应。
 *
 * @param features 启用特性得分列表；未传排序时保持启用特性排序。
 */
public record NssFeatureScoresResponse(
        List<NssFeatureScoreResponse> features
) {
}
