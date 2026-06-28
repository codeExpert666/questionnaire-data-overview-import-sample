package com.acme.questionnaire.dto;

import java.util.List;

public record TablePageResponse<T>(
        List<TableColumnResponse> columns,
        int pageNo,
        int pageSize,
        long total,
        List<T> rows
) {
}
