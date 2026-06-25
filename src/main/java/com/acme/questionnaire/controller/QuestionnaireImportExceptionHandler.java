package com.acme.questionnaire.controller;

import com.acme.questionnaire.dto.ApiErrorResponse;
import com.acme.questionnaire.dto.ImportErrorResponse;
import com.acme.questionnaire.exception.ExcelImportValidationException;
import com.acme.questionnaire.exception.QuestionnaireFeatureException;
import com.acme.questionnaire.exception.QuestionnaireImportException;
import com.acme.questionnaire.exception.QuestionnaireProductFeatureException;
import com.acme.questionnaire.exception.QuestionnaireProductException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 问卷模块异常响应转换器。
 *
 * <p>Excel 导入校验错误和系统错误使用 ImportErrorResponse，保留导入错误明细结构；产品、特性和
 * 产品特性维护接口使用通用 ApiErrorResponse。这样前端可以按接口类型区分展示方式。</p>
 */
@RestControllerAdvice
public class QuestionnaireImportExceptionHandler {

    /**
     * 处理上传文件内容或模板规则错误。
     *
     * <p>返回 400，表示调用方需要修正 Excel 内容后重新上传；errors 中携带行列级明细。</p>
     */
    @ExceptionHandler(ExcelImportValidationException.class)
    public ResponseEntity<ImportErrorResponse> handleValidation(
            ExcelImportValidationException exception) {
        return ResponseEntity.badRequest().body(new ImportErrorResponse(
                "QUESTIONNAIRE_IMPORT_VALIDATION_FAILED",
                exception.getMessage(),
                exception.getErrors()));
    }

    /**
     * 处理导入过程中的系统性失败。
     *
     * <p>例如读取文件失败、EasyExcel 无法解析工作簿或写库主键回查异常。此类错误不携带行级明细，
     * 通常需要排查文件格式、服务端依赖或数据库状态。</p>
     */
    @ExceptionHandler(QuestionnaireImportException.class)
    public ResponseEntity<ImportErrorResponse> handleImportSystemError(
            QuestionnaireImportException exception) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ImportErrorResponse(
                        "QUESTIONNAIRE_IMPORT_FAILED",
                        exception.getMessage(),
                        null));
    }

    @ExceptionHandler(QuestionnaireFeatureException.class)
    public ResponseEntity<ApiErrorResponse> handleFeatureError(
            QuestionnaireFeatureException exception) {
        return ResponseEntity.status(exception.getStatus())
                .body(new ApiErrorResponse(
                        exception.getCode(),
                        exception.getMessage()));
    }

    @ExceptionHandler(QuestionnaireProductException.class)
    public ResponseEntity<ApiErrorResponse> handleProductError(
            QuestionnaireProductException exception) {
        return ResponseEntity.status(exception.getStatus())
                .body(new ApiErrorResponse(
                        exception.getCode(),
                        exception.getMessage()));
    }

    @ExceptionHandler(QuestionnaireProductFeatureException.class)
    public ResponseEntity<ApiErrorResponse> handleProductFeatureError(
            QuestionnaireProductFeatureException exception) {
        return ResponseEntity.status(exception.getStatus())
                .body(new ApiErrorResponse(
                        exception.getCode(),
                        exception.getMessage()));
    }
}
