package com.acme.questionnaire.exception;

public class QuestionnaireImportException extends RuntimeException {
    public QuestionnaireImportException(String message) {
        super(message);
    }

    public QuestionnaireImportException(String message, Throwable cause) {
        super(message, cause);
    }
}
