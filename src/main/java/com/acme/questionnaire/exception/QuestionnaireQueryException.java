package com.acme.questionnaire.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class QuestionnaireQueryException extends RuntimeException {
    private final String code;
    private final HttpStatus status;

    public QuestionnaireQueryException(String code, String message, HttpStatus status) {
        super(message);
        this.code = code;
        this.status = status;
    }

    public static QuestionnaireQueryException invalid(String message) {
        return new QuestionnaireQueryException(
                "QUESTIONNAIRE_QUERY_INVALID",
                message,
                HttpStatus.BAD_REQUEST);
    }

    public static QuestionnaireQueryException featureNotFound(Long featureId) {
        return new QuestionnaireQueryException(
                "QUESTIONNAIRE_QUERY_FEATURE_NOT_FOUND",
                "特性不存在或已停用：" + featureId,
                HttpStatus.BAD_REQUEST);
    }
}
