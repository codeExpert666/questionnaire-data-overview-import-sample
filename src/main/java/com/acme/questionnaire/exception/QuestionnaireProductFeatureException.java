package com.acme.questionnaire.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * pq_product_feature 维护接口的业务异常。
 */
@Getter
public class QuestionnaireProductFeatureException extends RuntimeException {
    private final String code;
    private final HttpStatus status;

    public QuestionnaireProductFeatureException(String code,
                                                HttpStatus status,
                                                String message) {
        super(message);
        this.code = code;
        this.status = status;
    }
}
