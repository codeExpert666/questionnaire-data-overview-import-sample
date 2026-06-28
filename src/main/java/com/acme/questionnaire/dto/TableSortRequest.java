package com.acme.questionnaire.dto;

public record TableSortRequest(
        String field,
        String direction
) {
}
