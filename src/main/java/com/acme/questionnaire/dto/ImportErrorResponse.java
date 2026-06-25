package com.acme.questionnaire.dto;

import java.util.List;

/**
 * Excel 导入接口错误响应。
 *
 * <p>业务校验失败时 errors 为行列级明细；系统性失败时 errors 为 null，调用方应展示 message
 * 并提示重新检查文件格式或联系管理员。</p>
 */
public record ImportErrorResponse(String code, String message, List<ExcelImportError> errors) {
}
