package com.acme.questionnaire.controller;

import com.acme.questionnaire.dto.ScoreRowResponse;
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
@RequestMapping("/api/product-questionnaires/scores")
public class QuestionnaireScoreController {
    private final QuestionnaireTableQueryService queryService;

    @PostMapping("/query")
    public TablePageResponse<ScoreRowResponse> queryScores(
            @RequestBody(required = false) TableQueryRequest request) {
        return queryService.queryScores(request);
    }
}
