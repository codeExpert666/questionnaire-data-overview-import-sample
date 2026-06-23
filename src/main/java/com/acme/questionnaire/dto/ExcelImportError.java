package com.acme.questionnaire.dto;

public record ExcelImportError(Integer rowNumber, String columnName, String message) {
}
