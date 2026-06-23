package com.acme.questionnaire.service;

import com.acme.questionnaire.dto.ProductCreateRequest;
import com.acme.questionnaire.dto.ProductStatusRequest;
import com.acme.questionnaire.dto.ProductUpdateRequest;
import com.acme.questionnaire.exception.QuestionnaireProductException;
import com.acme.questionnaire.mapper.ProductMapper;
import com.acme.questionnaire.model.QuestionnaireProduct;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QuestionnaireProductServiceTest {
    @Mock
    private ProductMapper productMapper;

    @Mock
    private QuestionnaireCacheVersionService cacheVersionService;

    @InjectMocks
    private QuestionnaireProductService service;

    @Test
    void listProductsReturnsMapperRowsInOrder() {
        QuestionnaireProduct alpha = product(1L, "P100", "Alpha", 1);
        QuestionnaireProduct beta = product(2L, "P200", "Beta", 0);
        when(productMapper.selectAllProducts()).thenReturn(List.of(alpha, beta));

        var result = service.listProducts();

        assertThat(result).extracting("productCode").containsExactly("P100", "P200");
        assertThat(result).extracting("status").containsExactly(1, 0);
    }

    @Test
    void createProductRejectsDuplicateCode() {
        when(productMapper.existsByProductCode("P100")).thenReturn(true);

        assertThatThrownBy(() -> service.createProduct(
                new ProductCreateRequest(" P100 ", "Alpha", 1)))
                .isInstanceOfSatisfying(QuestionnaireProductException.class, ex -> {
                    assertThat(ex.getCode()).isEqualTo("QUESTIONNAIRE_PRODUCT_INVALID");
                    assertThat(ex.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(ex.getMessage()).contains("产品编码已存在");
                });

        verify(productMapper, never()).insertProduct(any());
        verify(cacheVersionService, never()).increaseAfterCommit();
    }

    @Test
    void createProductPersistsNormalizedValuesAndIncreasesCacheVersion() {
        when(productMapper.existsByProductCode("P100")).thenReturn(false);
        when(productMapper.selectById(42L)).thenReturn(product(42L, "P100", "Alpha", 1));
        ArgumentCaptor<QuestionnaireProduct> captor =
                ArgumentCaptor.forClass(QuestionnaireProduct.class);
        when(productMapper.insertProduct(captor.capture())).thenAnswer(invocation -> {
            captor.getValue().setId(42L);
            return 1;
        });

        var result = service.createProduct(
                new ProductCreateRequest(" P100 ", " Alpha ", null));

        assertThat(captor.getValue().getProductCode()).isEqualTo("P100");
        assertThat(captor.getValue().getProductModel()).isEqualTo("Alpha");
        assertThat(captor.getValue().getStatus()).isEqualTo(1);
        assertThat(result.id()).isEqualTo(42L);
        verify(cacheVersionService).increaseAfterCommit();
    }

    @Test
    void updateProductRejectsMissingProduct() {
        when(productMapper.selectById(99L)).thenReturn(null);

        assertThatThrownBy(() -> service.updateProduct(
                99L,
                new ProductUpdateRequest("Beta")))
                .isInstanceOfSatisfying(QuestionnaireProductException.class, ex -> {
                    assertThat(ex.getCode()).isEqualTo("QUESTIONNAIRE_PRODUCT_NOT_FOUND");
                    assertThat(ex.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                });

        verify(productMapper, never()).updateProduct(any());
        verify(cacheVersionService, never()).increaseAfterCommit();
    }

    @Test
    void updateProductKeepsProductCodeImmutableAndIncreasesCacheVersion() {
        when(productMapper.selectById(1L)).thenReturn(
                product(1L, "P100", "Alpha", 1),
                product(1L, "P100", "Alpha Pro", 1));
        ArgumentCaptor<QuestionnaireProduct> captor =
                ArgumentCaptor.forClass(QuestionnaireProduct.class);
        when(productMapper.updateProduct(captor.capture())).thenReturn(1);

        var result = service.updateProduct(1L, new ProductUpdateRequest(" Alpha Pro "));

        assertThat(captor.getValue().getId()).isEqualTo(1L);
        assertThat(captor.getValue().getProductCode()).isNull();
        assertThat(captor.getValue().getProductModel()).isEqualTo("Alpha Pro");
        assertThat(result.productCode()).isEqualTo("P100");
        assertThat(result.productModel()).isEqualTo("Alpha Pro");
        verify(cacheVersionService).increaseAfterCommit();
    }

    @Test
    void changeStatusSkipsWriteWhenStatusDoesNotChange() {
        when(productMapper.selectById(1L)).thenReturn(product(1L, "P100", "Alpha", 1));

        var result = service.changeStatus(1L, new ProductStatusRequest(1));

        assertThat(result.status()).isEqualTo(1);
        verify(productMapper, never()).updateProductStatus(any());
        verify(cacheVersionService, never()).increaseAfterCommit();
    }

    @Test
    void deleteProductSoftDeletesBySettingStatusToDisabled() {
        when(productMapper.selectById(1L)).thenReturn(
                product(1L, "P100", "Alpha", 1),
                product(1L, "P100", "Alpha", 0));
        ArgumentCaptor<QuestionnaireProduct> captor =
                ArgumentCaptor.forClass(QuestionnaireProduct.class);
        when(productMapper.updateProductStatus(captor.capture())).thenReturn(1);

        var result = service.deleteProduct(1L);

        assertThat(captor.getValue().getId()).isEqualTo(1L);
        assertThat(captor.getValue().getStatus()).isEqualTo(0);
        assertThat(result.status()).isEqualTo(0);
        verify(cacheVersionService).increaseAfterCommit();
    }

    private QuestionnaireProduct product(Long id,
                                         String productCode,
                                         String productModel,
                                         Integer status) {
        QuestionnaireProduct product = new QuestionnaireProduct();
        product.setId(id);
        product.setProductCode(productCode);
        product.setProductModel(productModel);
        product.setStatus(status);
        product.setCreatedAt(LocalDateTime.of(2026, 6, 23, 10, 0));
        product.setUpdatedAt(LocalDateTime.of(2026, 6, 23, 10, 0));
        return product;
    }
}
