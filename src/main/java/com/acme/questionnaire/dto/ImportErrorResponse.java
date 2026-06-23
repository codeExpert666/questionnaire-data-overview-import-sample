package com.acme.questionnaire.dto;

import java.util.List;

public record ImportErrorResponse(String code, String message, List<ExcelImportError> errors) {
}
