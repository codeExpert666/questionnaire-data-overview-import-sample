package com.acme.questionnaire.exception;

import com.acme.questionnaire.dto.ExcelImportError;
import lombok.Getter;

import java.util.List;

/**
 * Excel 导入业务校验异常。
 *
 * <p>用于表达用户上传内容本身不符合模板或业务规则，例如表头不匹配、必填缺失、产品或特性不存在。
 * 该异常会被控制器异常处理器转换为 400，并返回 errors 明细给前端逐行展示。</p>
 */
@Getter
public class ExcelImportValidationException extends RuntimeException {
    /** 不可变错误明细列表，避免异常抛出后被调用方继续修改。 */
    private final List<ExcelImportError> errors;

    public ExcelImportValidationException(String message, List<ExcelImportError> errors) {
        super(message);
        this.errors = List.copyOf(errors);
    }

    /**
     * 构造单条校验错误。
     *
     * <p>用于上传文件为空、文件类型错误等没有具体 Excel 行列上下文的场景；rowNumber 和 columnName
     * 可以为 null，由前端按全局错误展示。</p>
     */
    public static ExcelImportValidationException single(Integer rowNumber,
                                                        String columnName,
                                                        String message) {
        return new ExcelImportValidationException(
                "问卷观点表导入校验失败",
                List.of(new ExcelImportError(rowNumber, columnName, message)));
    }
}
