package com.acme.questionnaire.controller;

import com.acme.questionnaire.dto.AnalyticsFeatureOptionResponse;
import com.acme.questionnaire.dto.AnalyticsProductOptionResponse;
import com.acme.questionnaire.dto.ScoreAnalyticsFilterOptionsResponse;
import com.acme.questionnaire.service.QuestionnaireScoreAnalyticsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class QuestionnaireScoreAnalyticsControllerTest {
    private MockMvc mockMvc;

    @Mock
    private QuestionnaireScoreAnalyticsService analyticsService;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new QuestionnaireScoreAnalyticsController(analyticsService))
                .setControllerAdvice(new QuestionnaireImportExceptionHandler())
                .build();
    }

    @Test
    void filterOptionsEndpointReturnsProductsVersionsAndEnabledFeatures() throws Exception {
        when(analyticsService.queryFilterOptions()).thenReturn(new ScoreAnalyticsFilterOptionsResponse(
                List.of(new AnalyticsProductOptionResponse(1L, "P1", "Alpha")),
                List.of("8.1.0"),
                List.of("OS1.0"),
                List.of(new AnalyticsFeatureOptionResponse(7L, "F7", "续航"))));

        mockMvc.perform(get("/api/product-questionnaires/scores/analytics/filter-options"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.products[0].productId").value(1))
                .andExpect(jsonPath("$.products[0].productCode").value("P1"))
                .andExpect(jsonPath("$.products[0].productModel").value("Alpha"))
                .andExpect(jsonPath("$.appVersions[0]").value("8.1.0"))
                .andExpect(jsonPath("$.romVersions[0]").value("OS1.0"))
                .andExpect(jsonPath("$.features[0].featureId").value(7))
                .andExpect(jsonPath("$.features[0].featureCode").value("F7"))
                .andExpect(jsonPath("$.features[0].featureName").value("续航"));
    }
}
