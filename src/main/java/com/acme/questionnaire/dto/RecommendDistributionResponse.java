package com.acme.questionnaire.dto;

/**
 * 推荐意愿三类人群数量分布。
 *
 * @param promoterCount 推荐者数量，推荐意愿评分为 9 到 10。
 * @param passiveCount 中立者数量，推荐意愿评分为 7 到 8。
 * @param detractorCount 贬损者数量，推荐意愿评分为 1 到 6。
 */
public record RecommendDistributionResponse(
        long promoterCount,
        long passiveCount,
        long detractorCount
) {
}
