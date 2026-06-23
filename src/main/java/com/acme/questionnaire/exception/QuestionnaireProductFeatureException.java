package com.acme.questionnaire.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * pq_product_feature 维护接口的业务异常。
 *
 * <p>服务层通过该异常区分“产品不存在”和“特性集合非法”等配置错误。控制器异常处理器会把
 * code、HTTP status 和 message 转换为统一 API 错误响应。</p>
 */
@Getter
public class QuestionnaireProductFeatureException extends RuntimeException {
    /** 面向前端和调用方的稳定错误编码。 */
    private final String code;

    /** 本次业务错误应返回的 HTTP 状态。 */
    private final HttpStatus status;

    /**
     * 创建产品特性配置异常。
     *
     * @param code 稳定错误编码，例如特性集合非法或产品不存在
     * @param status HTTP 状态，用于 API 响应
     * @param message 人工可读错误信息
     */
    public QuestionnaireProductFeatureException(String code,
                                                HttpStatus status,
                                                String message) {
        super(message);
        this.code = code;
        this.status = status;
    }
}
