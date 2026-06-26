package com.acme.questionnaire.service;

import com.acme.questionnaire.dto.FeatureCreateRequest;
import com.acme.questionnaire.dto.FeatureStatusRequest;
import com.acme.questionnaire.dto.FeatureUpdateRequest;
import com.acme.questionnaire.exception.QuestionnaireFeatureException;
import com.acme.questionnaire.mapper.FeatureMapper;
import com.acme.questionnaire.model.QuestionnaireFeature;
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
class QuestionnaireFeatureServiceTest {
    @Mock
    private FeatureMapper featureMapper;

    @Mock
    private QuestionnaireCacheVersionService cacheVersionService;

    @InjectMocks
    private QuestionnaireFeatureService service;

    @Test
    void listFeaturesReturnsMapperRowsInOrder() {
        QuestionnaireFeature battery = feature(1L, "BATTERY", "续航", 10, 1);
        QuestionnaireFeature camera = feature(2L, "CAMERA", "影像", 20, 0);
        when(featureMapper.selectAllFeatures()).thenReturn(List.of(battery, camera));

        var result = service.listFeatures();

        assertThat(result).extracting("featureCode").containsExactly("BATTERY", "CAMERA");
        assertThat(result).extracting("status").containsExactly(1, 0);
    }

    @Test
    void createFeatureRejectsDuplicateCode() {
        when(featureMapper.existsByFeatureCode("BATTERY")).thenReturn(true);

        assertThatThrownBy(() -> service.createFeature(
                new FeatureCreateRequest(" BATTERY ", "续航", 10, 1)))
                .isInstanceOfSatisfying(QuestionnaireFeatureException.class, ex -> {
                    assertThat(ex.getCode()).isEqualTo("QUESTIONNAIRE_FEATURE_INVALID");
                    assertThat(ex.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(ex.getMessage()).contains("特性编码已存在");
                });

        verify(featureMapper, never()).insertFeature(any());
        verify(cacheVersionService, never()).increaseAfterCommit();
    }

    @Test
    void createFeatureRejectsDuplicateName() {
        when(featureMapper.existsByFeatureCode("BATTERY")).thenReturn(false);
        when(featureMapper.existsByFeatureName("续航")).thenReturn(true);

        assertThatThrownBy(() -> service.createFeature(
                new FeatureCreateRequest(" BATTERY ", " 续航 ", 10, 1)))
                .isInstanceOfSatisfying(QuestionnaireFeatureException.class, ex -> {
                    assertThat(ex.getCode()).isEqualTo("QUESTIONNAIRE_FEATURE_INVALID");
                    assertThat(ex.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(ex.getMessage()).contains("特性名称已存在");
                });

        verify(featureMapper, never()).insertFeature(any());
        verify(cacheVersionService, never()).increaseAfterCommit();
    }

    @Test
    void createFeaturePersistsNormalizedValuesAndIncreasesCacheVersion() {
        when(featureMapper.existsByFeatureCode("BATTERY")).thenReturn(false);
        when(featureMapper.selectById(42L)).thenReturn(feature(42L, "BATTERY", "续航", 10, 1));
        ArgumentCaptor<QuestionnaireFeature> captor =
                ArgumentCaptor.forClass(QuestionnaireFeature.class);
        when(featureMapper.insertFeature(captor.capture())).thenAnswer(invocation -> {
            captor.getValue().setId(42L);
            return 1;
        });

        var result = service.createFeature(
                new FeatureCreateRequest(" BATTERY ", " 续航 ", 10, null));

        assertThat(captor.getValue().getFeatureCode()).isEqualTo("BATTERY");
        assertThat(captor.getValue().getFeatureName()).isEqualTo("续航");
        assertThat(captor.getValue().getSortNo()).isEqualTo(10);
        assertThat(captor.getValue().getStatus()).isEqualTo(1);
        assertThat(result.id()).isEqualTo(42L);
        verify(cacheVersionService).increaseAfterCommit();
    }

    @Test
    void createFeatureAutoGeneratesCodeWhenFeatureCodeMissing() {
        when(featureMapper.selectById(42L)).thenReturn(feature(42L, "F42", "续航", 10, 1));
        ArgumentCaptor<QuestionnaireFeature> insertCaptor =
                ArgumentCaptor.forClass(QuestionnaireFeature.class);
        ArgumentCaptor<QuestionnaireFeature> updateCaptor =
                ArgumentCaptor.forClass(QuestionnaireFeature.class);
        when(featureMapper.insertFeature(insertCaptor.capture())).thenAnswer(invocation -> {
            insertCaptor.getValue().setId(42L);
            return 1;
        });
        when(featureMapper.updateFeatureCode(updateCaptor.capture())).thenReturn(1);

        var result = service.createFeature(
                new FeatureCreateRequest(" ", " 续航 ", 10, null));

        assertThat(insertCaptor.getValue().getFeatureCode()).startsWith("__AUTO_F_");
        assertThat(insertCaptor.getValue().getFeatureName()).isEqualTo("续航");
        assertThat(insertCaptor.getValue().getSortNo()).isEqualTo(10);
        assertThat(insertCaptor.getValue().getStatus()).isEqualTo(1);
        assertThat(updateCaptor.getValue().getId()).isEqualTo(42L);
        assertThat(updateCaptor.getValue().getFeatureCode()).isEqualTo("F42");
        assertThat(result.featureCode()).isEqualTo("F42");
        verify(cacheVersionService).increaseAfterCommit();
    }

    @Test
    void updateFeatureRejectsMissingFeature() {
        when(featureMapper.selectById(99L)).thenReturn(null);

        assertThatThrownBy(() -> service.updateFeature(
                99L,
                new FeatureUpdateRequest("影像", 20)))
                .isInstanceOfSatisfying(QuestionnaireFeatureException.class, ex -> {
                    assertThat(ex.getCode()).isEqualTo("QUESTIONNAIRE_FEATURE_NOT_FOUND");
                    assertThat(ex.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                });

        verify(featureMapper, never()).updateFeature(any());
        verify(cacheVersionService, never()).increaseAfterCommit();
    }

    @Test
    void updateFeatureRejectsDuplicateNameFromOtherFeature() {
        when(featureMapper.selectById(1L)).thenReturn(feature(1L, "BATTERY", "续航", 10, 1));
        when(featureMapper.existsByFeatureNameExceptId("影像", 1L)).thenReturn(true);

        assertThatThrownBy(() -> service.updateFeature(
                1L,
                new FeatureUpdateRequest(" 影像 ", 20)))
                .isInstanceOfSatisfying(QuestionnaireFeatureException.class, ex -> {
                    assertThat(ex.getCode()).isEqualTo("QUESTIONNAIRE_FEATURE_INVALID");
                    assertThat(ex.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(ex.getMessage()).contains("特性名称已存在");
                });

        verify(featureMapper, never()).updateFeature(any());
        verify(cacheVersionService, never()).increaseAfterCommit();
    }

    @Test
    void updateFeatureKeepsFeatureCodeImmutableAndIncreasesCacheVersion() {
        when(featureMapper.selectById(1L)).thenReturn(
                feature(1L, "BATTERY", "续航", 10, 1),
                feature(1L, "BATTERY", "续航体验", 30, 1));
        ArgumentCaptor<QuestionnaireFeature> captor =
                ArgumentCaptor.forClass(QuestionnaireFeature.class);
        when(featureMapper.updateFeature(captor.capture())).thenReturn(1);

        var result = service.updateFeature(1L, new FeatureUpdateRequest(" 续航体验 ", 30));

        assertThat(captor.getValue().getId()).isEqualTo(1L);
        assertThat(captor.getValue().getFeatureCode()).isNull();
        assertThat(captor.getValue().getFeatureName()).isEqualTo("续航体验");
        assertThat(captor.getValue().getSortNo()).isEqualTo(30);
        assertThat(result.featureCode()).isEqualTo("BATTERY");
        assertThat(result.featureName()).isEqualTo("续航体验");
        verify(cacheVersionService).increaseAfterCommit();
    }

    @Test
    void changeStatusSkipsWriteWhenStatusDoesNotChange() {
        when(featureMapper.selectById(1L)).thenReturn(feature(1L, "BATTERY", "续航", 10, 1));

        var result = service.changeStatus(1L, new FeatureStatusRequest(1));

        assertThat(result.status()).isEqualTo(1);
        verify(featureMapper, never()).updateFeatureStatus(any());
        verify(cacheVersionService, never()).increaseAfterCommit();
    }

    @Test
    void deleteFeatureSoftDeletesBySettingStatusToDisabled() {
        when(featureMapper.selectById(1L)).thenReturn(
                feature(1L, "BATTERY", "续航", 10, 1),
                feature(1L, "BATTERY", "续航", 10, 0));
        ArgumentCaptor<QuestionnaireFeature> captor =
                ArgumentCaptor.forClass(QuestionnaireFeature.class);
        when(featureMapper.updateFeatureStatus(captor.capture())).thenReturn(1);

        var result = service.deleteFeature(1L);

        assertThat(captor.getValue().getId()).isEqualTo(1L);
        assertThat(captor.getValue().getStatus()).isEqualTo(0);
        assertThat(result.status()).isEqualTo(0);
        verify(cacheVersionService).increaseAfterCommit();
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
