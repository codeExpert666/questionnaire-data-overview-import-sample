package com.acme.questionnaire.controller;

import com.acme.questionnaire.dto.OpinionRowResponse;
import com.acme.questionnaire.dto.TablePageResponse;
import com.acme.questionnaire.dto.TableQueryRequest;
import com.acme.questionnaire.service.QuestionnaireTableQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/product-questionnaires/opinions")
public class QuestionnaireOpinionController {
    private final QuestionnaireTableQueryService queryService;

    @PostMapping("/query")
    public TablePageResponse<OpinionRowResponse> queryOpinions(
            @RequestBody(required = false) TableQueryRequest request) {
        return queryService.queryOpinions(request);
    }
}
