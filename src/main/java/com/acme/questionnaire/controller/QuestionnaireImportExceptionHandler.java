package com.acme.questionnaire.controller;

import com.acme.questionnaire.dto.ApiErrorResponse;
import com.acme.questionnaire.dto.ImportErrorResponse;
import com.acme.questionnaire.exception.ExcelImportValidationException;
import com.acme.questionnaire.exception.QuestionnaireFeatureException;
import com.acme.questionnaire.exception.QuestionnaireImportException;
import com.acme.questionnaire.exception.QuestionnaireProductException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class QuestionnaireImportExceptionHandler {

    @ExceptionHandler(ExcelImportValidationException.class)
    public ResponseEntity<ImportErrorResponse> handleValidation(
            ExcelImportValidationException exception) {
        return ResponseEntity.badRequest().body(new ImportErrorResponse(
                "QUESTIONNAIRE_IMPORT_VALIDATION_FAILED",
                exception.getMessage(),
                exception.getErrors()));
    }

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
}
