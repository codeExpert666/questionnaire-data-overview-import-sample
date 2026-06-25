package com.acme.questionnaire.service;

import com.acme.questionnaire.dto.ProductFeatureSaveRequest;
import com.acme.questionnaire.exception.QuestionnaireProductFeatureException;
import com.acme.questionnaire.mapper.FeatureMapper;
import com.acme.questionnaire.mapper.ProductFeatureMapper;
import com.acme.questionnaire.mapper.ProductMapper;
import com.acme.questionnaire.model.QuestionnaireFeature;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QuestionnaireProductFeatureServiceTest {
    @Mock
    private ProductMapper productMapper;

    @Mock
    private FeatureMapper featureMapper;

    @Mock
    private ProductFeatureMapper productFeatureMapper;

    @Mock
    private QuestionnaireCacheVersionService cacheVersionService;

    @InjectMocks
    private QuestionnaireProductFeatureService service;

    @Test
    void listProductFeaturesOnlyReturnsEnabledFeaturesWithSelectedFlags() {
        when(productMapper.selectById(1L)).thenReturn(product(1L, "P100", "Alpha", 1));
        when(featureMapper.selectAllFeatures()).thenReturn(List.of(
                feature(1L, "BATTERY", "续航", 10, 1),
                feature(2L, "CAMERA", "影像", 20, 0)));
        when(productFeatureMapper.selectEnabledFeatureIdsByProductId(1L)).thenReturn(List.of(1L, 2L));

        var result = service.listProductFeatures(1L);

        assertThat(result.productId()).isEqualTo(1L);
        assertThat(result.productCode()).isEqualTo("P100");
        assertThat(result.features()).hasSize(1);
        assertThat(result.features()).extracting("id").containsExactly(1L);
        assertThat(result.features()).extracting("selected").containsExactly(true);
    }

    @Test
    void listProductFeaturesRejectsDisabledProduct() {
        when(productMapper.selectById(1L)).thenReturn(product(1L, "P100", "Alpha", 0));

        assertThatThrownBy(() -> service.listProductFeatures(1L))
                .isInstanceOfSatisfying(QuestionnaireProductFeatureException.class, ex -> {
                    assertThat(ex.getCode()).isEqualTo("QUESTIONNAIRE_PRODUCT_FEATURE_INVALID");
                    assertThat(ex.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(ex.getMessage()).contains("产品已停用：1");
                });

        verify(featureMapper, never()).selectAllFeatures();
        verify(productFeatureMapper, never()).selectEnabledFeatureIdsByProductId(1L);
    }

    @Test
    void saveProductFeaturesRejectsMissingProduct() {
        when(productMapper.selectById(99L)).thenReturn(null);

        assertThatThrownBy(() -> service.saveProductFeatures(
                99L,
                new ProductFeatureSaveRequest(List.of(1L))))
                .isInstanceOfSatisfying(QuestionnaireProductFeatureException.class, ex -> {
                    assertThat(ex.getCode()).isEqualTo("QUESTIONNAIRE_PRODUCT_FEATURE_NOT_FOUND");
                    assertThat(ex.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                });

        verify(productFeatureMapper, never()).disableAllByProductId(99L);
        verify(cacheVersionService, never()).increaseAfterCommit();
    }

    @Test
    void saveProductFeaturesRejectsDisabledProduct() {
        when(productMapper.selectById(1L)).thenReturn(product(1L, "P100", "Alpha", 0));

        assertThatThrownBy(() -> service.saveProductFeatures(
                1L,
                new ProductFeatureSaveRequest(List.of(1L))))
                .isInstanceOfSatisfying(QuestionnaireProductFeatureException.class, ex -> {
                    assertThat(ex.getCode()).isEqualTo("QUESTIONNAIRE_PRODUCT_FEATURE_INVALID");
                    assertThat(ex.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(ex.getMessage()).contains("产品已停用：1");
                });

        verify(featureMapper, never()).selectAllFeatures();
        verify(productFeatureMapper, never()).disableAllByProductId(1L);
        verify(cacheVersionService, never()).increaseAfterCommit();
    }

    @Test
    void saveProductFeaturesRejectsUnknownFeatureId() {
        when(productMapper.selectById(1L)).thenReturn(product(1L, "P100", "Alpha", 1));
        when(featureMapper.selectAllFeatures()).thenReturn(List.of(
                feature(1L, "BATTERY", "续航", 10, 1)));

        assertThatThrownBy(() -> service.saveProductFeatures(
                1L,
                new ProductFeatureSaveRequest(List.of(1L, 2L))))
                .isInstanceOfSatisfying(QuestionnaireProductFeatureException.class, ex -> {
                    assertThat(ex.getCode()).isEqualTo("QUESTIONNAIRE_PRODUCT_FEATURE_INVALID");
                    assertThat(ex.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(ex.getMessage()).contains("特性不存在：2");
                });

        verify(productFeatureMapper, never()).disableAllByProductId(1L);
        verify(cacheVersionService, never()).increaseAfterCommit();
    }

    @Test
    void saveProductFeaturesRejectsDisabledFeatureId() {
        when(productMapper.selectById(1L)).thenReturn(product(1L, "P100", "Alpha", 1));
        when(featureMapper.selectAllFeatures()).thenReturn(List.of(
                feature(2L, "CAMERA", "影像", 20, 0)));

        assertThatThrownBy(() -> service.saveProductFeatures(
                1L,
                new ProductFeatureSaveRequest(List.of(2L))))
                .isInstanceOfSatisfying(QuestionnaireProductFeatureException.class, ex -> {
                    assertThat(ex.getCode()).isEqualTo("QUESTIONNAIRE_PRODUCT_FEATURE_INVALID");
                    assertThat(ex.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(ex.getMessage()).contains("特性已停用：2");
                });

        verify(productFeatureMapper, never()).disableAllByProductId(1L);
        verify(cacheVersionService, never()).increaseAfterCommit();
    }

    @Test
    void saveProductFeaturesSkipsWriteWhenSelectionUnchanged() {
        when(productMapper.selectById(1L)).thenReturn(product(1L, "P100", "Alpha", 1));
        when(featureMapper.selectAllFeatures()).thenReturn(List.of(
                feature(1L, "BATTERY", "续航", 10, 1),
                feature(2L, "CAMERA", "影像", 20, 1)));
        when(productFeatureMapper.selectEnabledFeatureIdsByProductId(1L))
                .thenReturn(List.of(1L, 2L));

        var result = service.saveProductFeatures(
                1L,
                new ProductFeatureSaveRequest(List.of(2L, 1L, 1L)));

        assertThat(result.features()).extracting("id").containsExactly(1L, 2L);
        assertThat(result.features()).extracting("selected").containsExactly(true, true);
        verify(productFeatureMapper, never()).disableAllByProductId(1L);
        verify(productFeatureMapper, never()).upsertProductFeatures(1L, List.of(1L, 2L));
        verify(cacheVersionService, never()).increaseAfterCommit();
    }

    @Test
    @SuppressWarnings("unchecked")
    void saveProductFeaturesReplacesEnabledFeatureSetAndIncreasesCacheVersion() {
        when(productMapper.selectById(1L)).thenReturn(product(1L, "P100", "Alpha", 1));
        when(featureMapper.selectAllFeatures()).thenReturn(List.of(
                feature(1L, "BATTERY", "续航", 10, 1),
                feature(2L, "CAMERA", "影像", 20, 1),
                feature(3L, "SCREEN", "屏幕", 30, 1)));
        when(productFeatureMapper.selectEnabledFeatureIdsByProductId(1L))
                .thenReturn(List.of(1L, 3L));
        ArgumentCaptor<List<Long>> featureIdsCaptor = ArgumentCaptor.forClass(List.class);

        var result = service.saveProductFeatures(
                1L,
                new ProductFeatureSaveRequest(List.of(2L, 1L)));

        verify(productFeatureMapper).disableAllByProductId(1L);
        verify(productFeatureMapper).upsertProductFeatures(
                org.mockito.ArgumentMatchers.eq(1L),
                featureIdsCaptor.capture());
        assertThat(featureIdsCaptor.getValue()).containsExactly(1L, 2L);
        assertThat(result.features()).extracting("id").containsExactly(1L, 2L, 3L);
        assertThat(result.features()).extracting("selected").containsExactly(true, true, false);
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

    private QuestionnaireFeature feature(Long id,
                                         String featureCode,
                                         String featureName,
                                         Integer sortNo,
                                         Integer status) {
        QuestionnaireFeature feature = new QuestionnaireFeature();
        feature.setId(id);
        feature.setFeatureCode(featureCode);
        feature.setFeatureName(featureName);
        feature.setSortNo(sortNo);
        feature.setStatus(status);
        feature.setCreatedAt(LocalDateTime.of(2026, 6, 23, 10, 0));
        feature.setUpdatedAt(LocalDateTime.of(2026, 6, 23, 10, 0));
        return feature;
    }
}
