package com.acme.questionnaire.exception;

import com.acme.questionnaire.dto.ExcelImportError;
import lombok.Getter;

import java.util.List;

@Getter
public class ExcelImportValidationException extends RuntimeException {
    private final List<ExcelImportError> errors;

    public ExcelImportValidationException(String message, List<ExcelImportError> errors) {
        super(message);
        this.errors = List.copyOf(errors);
    }

    public static ExcelImportValidationException single(Integer rowNumber,
                                                        String columnName,
                                                        String message) {
        return new ExcelImportValidationException(
                "问卷观点表导入校验失败",
                List.of(new ExcelImportError(rowNumber, columnName, message)));
    }
}
