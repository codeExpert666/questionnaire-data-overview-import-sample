package com.acme.questionnaire.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * pq_product 维护接口的业务异常。
 *
 * <p>Service 用该异常区分参数错误、重复编码和产品不存在等可预期错误；全局异常处理器会把
 * code、HTTP 状态和 message 转成统一 JSON，避免 Controller 里散落错误响应拼装逻辑。</p>
 */
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
