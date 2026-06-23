package com.acme.questionnaire.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class QuestionnaireFeatureException extends RuntimeException {
    private final String code;
    private final HttpStatus status;

    public QuestionnaireFeatureException(String code, HttpStatus status, String message) {
        super(message);
        this.code = code;
        this.status = status;
    }
}
