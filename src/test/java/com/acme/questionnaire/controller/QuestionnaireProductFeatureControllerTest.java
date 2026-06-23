package com.acme.questionnaire.controller;

import com.acme.questionnaire.dto.ProductFeatureConfigurationResponse;
import com.acme.questionnaire.dto.ProductFeatureOptionResponse;
import com.acme.questionnaire.dto.ProductFeatureSaveRequest;
import com.acme.questionnaire.exception.QuestionnaireProductFeatureException;
import com.acme.questionnaire.service.QuestionnaireProductFeatureService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class QuestionnaireProductFeatureControllerTest {
    private MockMvc mockMvc;

    @Mock
    private QuestionnaireProductFeatureService productFeatureService;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new QuestionnaireProductFeatureController(productFeatureService))
                .setControllerAdvice(new QuestionnaireImportExceptionHandler())
                .build();
    }

    @Test
    void listProductFeaturesReturnsConfigurationJson() throws Exception {
        when(productFeatureService.listProductFeatures(1L)).thenReturn(configuration());

        mockMvc.perform(get("/api/product-questionnaires/products/1/features"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").value(1))
                .andExpect(jsonPath("$.productCode").value("P100"))
                .andExpect(jsonPath("$.features[0].id").value(1))
                .andExpect(jsonPath("$.features[0].featureCode").value("BATTERY"))
                .andExpect(jsonPath("$.features[0].selected").value(true));
    }

    @Test
    void saveProductFeaturesAcceptsFeatureIdSet() throws Exception {
        when(productFeatureService.saveProductFeatures(
                eq(1L),
                any(ProductFeatureSaveRequest.class))).thenReturn(configuration());

        mockMvc.perform(put("/api/product-questionnaires/products/1/features")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "featureIds": [1, 2]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").value(1))
                .andExpect(jsonPath("$.features[0].selected").value(true));
    }

    @Test
    void productFeatureExceptionReturnsApiErrorJson() throws Exception {
        when(productFeatureService.saveProductFeatures(
                eq(1L),
                any(ProductFeatureSaveRequest.class)))
                .thenThrow(new QuestionnaireProductFeatureException(
                        "QUESTIONNAIRE_PRODUCT_FEATURE_INVALID",
                        HttpStatus.BAD_REQUEST,
                        "特性不存在：99"));

        mockMvc.perform(put("/api/product-questionnaires/products/1/features")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "featureIds": [99]
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("QUESTIONNAIRE_PRODUCT_FEATURE_INVALID"))
                .andExpect(jsonPath("$.message").value("特性不存在：99"));
    }

    private ProductFeatureConfigurationResponse configuration() {
        return new ProductFeatureConfigurationResponse(
                1L,
                "P100",
                "Alpha",
                1,
                List.of(new ProductFeatureOptionResponse(
                        1L,
                        "BATTERY",
                        "续航",
                        10,
                        1,
                        true)));
    }
}
