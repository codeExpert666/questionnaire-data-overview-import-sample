package com.acme.questionnaire.exception;

/**
 * Excel 导入系统性异常。
 *
 * <p>用于表达不是用户行级数据校验导致的失败，例如文件读取失败、工作簿格式无法解析或写库后
 * 主键回查异常。该异常会被转换为 500，不返回 ExcelImportError 明细。</p>
 */
public class QuestionnaireImportException extends RuntimeException {
    public QuestionnaireImportException(String message) {
        super(message);
    }

    public QuestionnaireImportException(String message, Throwable cause) {
        super(message, cause);
    }
}
