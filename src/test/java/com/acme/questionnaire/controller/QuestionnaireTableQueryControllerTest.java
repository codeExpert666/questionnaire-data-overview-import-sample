package com.acme.questionnaire.controller;

import com.acme.questionnaire.dto.OpinionRowResponse;
import com.acme.questionnaire.dto.QuestionnaireImportResult;
import com.acme.questionnaire.dto.ScoreRowResponse;
import com.acme.questionnaire.dto.TablePageResponse;
import com.acme.questionnaire.dto.TableQueryRequest;
import com.acme.questionnaire.exception.QuestionnaireQueryException;
import com.acme.questionnaire.service.QuestionnaireDataOverviewExcelService;
import com.acme.questionnaire.service.QuestionnaireTableQueryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class QuestionnaireTableQueryControllerTest {
    private MockMvc mockMvc;

    @Mock
    private QuestionnaireDataOverviewExcelService excelService;
    @Mock
    private QuestionnaireTableQueryService queryService;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(
                        new QuestionnaireDataOverviewController(excelService, queryService),
                        new QuestionnaireScoreController(queryService),
                        new QuestionnaireOpinionController(queryService))
                .setControllerAdvice(new QuestionnaireImportExceptionHandler())
                .build();
    }

    @Test
    void queryScoresAcceptsJsonAndReturnsPageRows() throws Exception {
        ScoreRowResponse row = new ScoreRowResponse();
        row.setAnswerId(10L);
        row.setQuestionnaireId("Q001");
        row.setProductModel("Alpha");
        row.setProductCode("P100");
        row.setAnswerTime(LocalDateTime.of(2026, 6, 1, 9, 30));
        row.setRecommendScore(9);
        row.setUserCategory("推荐者");
        row.putFeatureScore(7L, 8);
        when(queryService.queryScores(any(TableQueryRequest.class)))
                .thenReturn(new TablePageResponse<>(List.of(), 1, 20, 1, List.of(row)));

        mockMvc.perform(post("/api/product-questionnaires/scores/query")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "pageNo": 1,
                                  "pageSize": 20,
                                  "filters": {
                                    "questionnaireId": "Q001"
                                  },
                                  "sorts": [
                                    {
                                      "field": "answerTime",
                                      "direction": "DESC"
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pageNo").value(1))
                .andExpect(jsonPath("$.pageSize").value(20))
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.rows[0].questionnaireId").value("Q001"))
                .andExpect(jsonPath("$.rows[0].recommendScore").value(9))
                .andExpect(jsonPath("$.rows[0]['featureScore:7']").value(8));
    }

    @Test
    void queryOpinionsAcceptsJsonAndReturnsPageRows() throws Exception {
        OpinionRowResponse row = new OpinionRowResponse();
        row.setOpinionId(20L);
        row.setAnswerId(10L);
        row.setQuestionnaireId("Q002");
        row.setProductModel("Beta");
        row.setProductCode("P200");
        row.setAnswerTime(LocalDateTime.of(2026, 6, 2, 10, 0));
        row.setRecommendScore(5);
        row.setUserCategory("中立者");
        row.setOpinionSeq(1);
        row.setFeatureName("续航");
        row.setSentiment("好评");
        row.setFeedbackContent1("续航时间长");
        row.setFeedbackContent2("充电稳定");
        when(queryService.queryOpinions(any(TableQueryRequest.class)))
                .thenReturn(new TablePageResponse<>(List.of(), 1, 20, 1, List.of(row)));

        mockMvc.perform(post("/api/product-questionnaires/opinions/query")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "pageNo": 1,
                                  "pageSize": 20,
                                  "filters": {
                                    "sentiment": 1
                                  }
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pageNo").value(1))
                .andExpect(jsonPath("$.rows[0].questionnaireId").value("Q002"))
                .andExpect(jsonPath("$.rows[0].sentiment").value("好评"))
                .andExpect(jsonPath("$.rows[0].feedbackContent1").value("续航时间长"));
    }

    @Test
    void queryDataOverviewMapsQueryExceptionToBadRequestApiError() throws Exception {
        when(queryService.queryDataOverview(any(TableQueryRequest.class)))
                .thenThrow(QuestionnaireQueryException.invalid("分页偏移量超出限制"));

        mockMvc.perform(post("/api/product-questionnaires/data-overview/query")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "pageNo": 2147483647,
                                  "pageSize": 200
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("QUESTIONNAIRE_QUERY_INVALID"))
                .andExpect(jsonPath("$.message").value("分页偏移量超出限制"));
    }

    @Test
    void renamedDataOverviewControllerStillDelegatesImport() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "questionnaire.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                new byte[] {1, 2, 3});
        when(excelService.importExcel(any()))
                .thenReturn(new QuestionnaireImportResult(1, 1, 2, 3));

        mockMvc.perform(multipart("/api/product-questionnaires/data-overview/import").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dataRowCount").value(1))
                .andExpect(jsonPath("$.questionnaireCount").value(1))
                .andExpect(jsonPath("$.opinionCount").value(2))
                .andExpect(jsonPath("$.featureScoreCount").value(3));
    }
}
