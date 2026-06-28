package com.acme.questionnaire.dto;

import java.util.List;

public record TableQueryRequest(
        Integer pageNo,
        Integer pageSize,
        TableQueryFilterRequest filters,
        List<FeatureScoreFilterRequest> featureScoreFilters,
        List<TableSortRequest> sorts
) {
}
