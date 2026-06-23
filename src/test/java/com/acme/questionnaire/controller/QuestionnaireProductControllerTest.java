package com.acme.questionnaire.controller;

import com.acme.questionnaire.dto.ProductCreateRequest;
import com.acme.questionnaire.dto.ProductResponse;
import com.acme.questionnaire.exception.QuestionnaireProductException;
import com.acme.questionnaire.service.QuestionnaireProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class QuestionnaireProductControllerTest {
    private MockMvc mockMvc;

    @Mock
    private QuestionnaireProductService productService;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new QuestionnaireProductController(productService))
                .setControllerAdvice(new QuestionnaireImportExceptionHandler())
                .build();
    }

    @Test
    void listProductsReturnsProductJson() throws Exception {
        when(productService.listProducts()).thenReturn(List.of(new ProductResponse(
                1L,
                "P100",
                "Alpha",
                1,
                LocalDateTime.of(2026, 6, 23, 10, 0),
                LocalDateTime.of(2026, 6, 23, 10, 0))));

        mockMvc.perform(get("/api/product-questionnaires/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].productCode").value("P100"))
                .andExpect(jsonPath("$[0].productModel").value("Alpha"))
                .andExpect(jsonPath("$[0].status").value(1));
    }

    @Test
    void productExceptionReturnsApiErrorJson() throws Exception {
        when(productService.createProduct(any(ProductCreateRequest.class)))
                .thenThrow(new QuestionnaireProductException(
                        "QUESTIONNAIRE_PRODUCT_INVALID",
                        HttpStatus.BAD_REQUEST,
                        "产品编码已存在：P100"));

        mockMvc.perform(post("/api/product-questionnaires/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "productCode": "P100",
                                  "productModel": "Alpha"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("QUESTIONNAIRE_PRODUCT_INVALID"))
                .andExpect(jsonPath("$.message").value("产品编码已存在：P100"));
    }
}
