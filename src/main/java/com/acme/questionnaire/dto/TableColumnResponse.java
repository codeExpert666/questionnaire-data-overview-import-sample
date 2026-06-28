package com.acme.questionnaire.dto;

public record TableColumnResponse(
        String key,
        String title,
        boolean sortable,
        boolean filterable
) {
}
