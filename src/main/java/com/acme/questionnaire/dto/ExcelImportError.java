package com.acme.questionnaire.dto;

/**
 * Excel 导入错误明细。
 *
 * @param rowNumber Excel 中的 1-based 行号；无法定位到具体行时为 null
 * @param columnName 业务列名；文件级错误或无法定位具体列时为 null
 * @param message 面向填表人的错误说明
 */
public record ExcelImportError(Integer rowNumber, String columnName, String message) {
}
