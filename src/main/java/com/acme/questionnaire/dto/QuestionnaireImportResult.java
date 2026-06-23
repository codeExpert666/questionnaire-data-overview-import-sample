package com.acme.questionnaire.dto;

public record QuestionnaireImportResult(
        int dataRowCount,
        int questionnaireCount,
        int opinionCount,
        int featureScoreCount
) {
}
