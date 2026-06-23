package com.acme.questionnaire.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class QuestionnaireProductException extends RuntimeException {
    private final String code;
    private final HttpStatus status;

    public QuestionnaireProductException(String code, HttpStatus status, String message) {
        super(message);
        this.code = code;
        this.status = status;
    }
}
