package com.acme.questionnaire.controller;

import com.acme.questionnaire.dto.FeatureCreateRequest;
import com.acme.questionnaire.dto.FeatureResponse;
import com.acme.questionnaire.exception.QuestionnaireFeatureException;
import com.acme.questionnaire.service.QuestionnaireFeatureService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class QuestionnaireFeatureControllerTest {
    private MockMvc mockMvc;

    @Mock
    private QuestionnaireFeatureService featureService;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new QuestionnaireFeatureController(featureService))
                .setControllerAdvice(new QuestionnaireImportExceptionHandler())
                .build();
    }

    @Test
    void listFeaturesReturnsFeatureJson() throws Exception {
        when(featureService.listFeatures()).thenReturn(List.of(new FeatureResponse(
                1L,
                "BATTERY",
                "续航",
                10,
                1,
                LocalDateTime.of(2026, 6, 23, 10, 0),
                LocalDateTime.of(2026, 6, 23, 10, 0))));

        mockMvc.perform(get("/api/product-questionnaires/features"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].featureCode").value("BATTERY"))
                .andExpect(jsonPath("$[0].featureName").value("续航"))
                .andExpect(jsonPath("$[0].sortNo").value(10))
                .andExpect(jsonPath("$[0].status").value(1));
    }

    @Test
    void featureExceptionReturnsApiErrorJson() throws Exception {
        when(featureService.createFeature(any(FeatureCreateRequest.class)))
                .thenThrow(new QuestionnaireFeatureException(
                        "QUESTIONNAIRE_FEATURE_INVALID",
                        HttpStatus.BAD_REQUEST,
                        "特性编码已存在：BATTERY"));

        mockMvc.perform(post("/api/product-questionnaires/features")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "featureCode": "BATTERY",
                                  "featureName": "续航",
                                  "sortNo": 10
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("QUESTIONNAIRE_FEATURE_INVALID"))
                .andExpect(jsonPath("$.message").value("特性编码已存在：BATTERY"));
    }
}
