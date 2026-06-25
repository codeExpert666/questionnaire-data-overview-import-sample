package com.acme.questionnaire.dto;

/**
 * Excel 导入成功结果。
 *
 * <p>只有整份文件通过校验并完成数据库写入后才会返回该对象。统计值来自实际写入批次，
 * 不包含失败文件中已经解析但最终回滚的数据。</p>
 *
 * @param dataRowCount 实际读取的数据行数，不含表头和被 EasyExcel 忽略的空行
 * @param questionnaireCount 成功写入的问卷聚合数量
 * @param opinionCount 成功写入的观点明细数量
 * @param featureScoreCount 成功写入的非空特性评分数量
 */
public record QuestionnaireImportResult(
        int dataRowCount,
        int questionnaireCount,
        int opinionCount,
        int featureScoreCount
) {
}
