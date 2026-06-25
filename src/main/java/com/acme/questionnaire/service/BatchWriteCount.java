package com.acme.questionnaire.service;

/**
 * 一次批量写库的成功计数。
 *
 * <p>监听器把每批返回值累加为接口成功响应；如果后续批次失败，外层事务仍会回滚全部写入。</p>
 *
 * @param questionnaireCount 写入或更新的问卷数量
 * @param opinionCount 插入的观点明细数量
 * @param featureScoreCount 插入的非空特性评分数量
 */
public record BatchWriteCount(int questionnaireCount, int opinionCount, int featureScoreCount) {
}
