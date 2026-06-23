package com.acme.questionnaire.service;

import com.acme.questionnaire.dto.FeatureCreateRequest;
import com.acme.questionnaire.dto.FeatureResponse;
import com.acme.questionnaire.dto.FeatureStatusRequest;
import com.acme.questionnaire.dto.FeatureUpdateRequest;
import com.acme.questionnaire.exception.QuestionnaireFeatureException;
import com.acme.questionnaire.mapper.FeatureMapper;
import com.acme.questionnaire.model.QuestionnaireFeature;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class QuestionnaireFeatureService {
    private static final String INVALID_CODE = "QUESTIONNAIRE_FEATURE_INVALID";
    private static final String NOT_FOUND_CODE = "QUESTIONNAIRE_FEATURE_NOT_FOUND";
    private static final int ENABLED = 1;
    private static final int DISABLED = 0;
    private static final int MAX_FEATURE_CODE_LENGTH = 64;
    private static final int MAX_FEATURE_NAME_LENGTH = 128;
    private static final Pattern FEATURE_CODE_PATTERN =
            Pattern.compile("^[A-Za-z0-9_.-]{1,64}$");

    private final FeatureMapper featureMapper;
    private final QuestionnaireCacheVersionService cacheVersionService;

    public List<FeatureResponse> listFeatures() {
        return featureMapper.selectAllFeatures().stream()
                .map(FeatureResponse::from)
                .toList();
    }

    @Transactional(rollbackFor = Exception.class)
    public FeatureResponse createFeature(FeatureCreateRequest request) {
        String featureCode = normalizeFeatureCode(request == null ? null : request.featureCode());
        String featureName = normalizeFeatureName(request == null ? null : request.featureName());
        int sortNo = normalizeSortNo(request == null ? null : request.sortNo());
        int status = normalizeStatus(request == null ? null : request.status(), ENABLED);

        if (featureMapper.existsByFeatureCode(featureCode)) {
            throw invalid("特性编码已存在：" + featureCode);
        }

        QuestionnaireFeature feature = new QuestionnaireFeature();
        feature.setFeatureCode(featureCode);
        feature.setFeatureName(featureName);
        feature.setSortNo(sortNo);
        feature.setStatus(status);
        try {
            featureMapper.insertFeature(feature);
        } catch (DuplicateKeyException ex) {
            throw invalid("特性编码已存在：" + featureCode);
        }

        cacheVersionService.increaseAfterCommit();
        return reloadFeature(feature.getId());
    }

    @Transactional(rollbackFor = Exception.class)
    public FeatureResponse updateFeature(Long id, FeatureUpdateRequest request) {
        requireExistingFeature(id);

        QuestionnaireFeature feature = new QuestionnaireFeature();
        feature.setId(id);
        feature.setFeatureName(normalizeFeatureName(request == null ? null : request.featureName()));
        feature.setSortNo(normalizeSortNo(request == null ? null : request.sortNo()));

        if (featureMapper.updateFeature(feature) == 0) {
            throw notFound(id);
        }

        cacheVersionService.increaseAfterCommit();
        return reloadFeature(id);
    }

    @Transactional(rollbackFor = Exception.class)
    public FeatureResponse changeStatus(Long id, FeatureStatusRequest request) {
        QuestionnaireFeature existing = requireExistingFeature(id);
        int status = normalizeStatus(request == null ? null : request.status(), null);
        if (existing.getStatus() != null && existing.getStatus() == status) {
            return FeatureResponse.from(existing);
        }

        QuestionnaireFeature feature = new QuestionnaireFeature();
        feature.setId(id);
        feature.setStatus(status);
        if (featureMapper.updateFeatureStatus(feature) == 0) {
            throw notFound(id);
        }

        cacheVersionService.increaseAfterCommit();
        return reloadFeature(id);
    }

    @Transactional(rollbackFor = Exception.class)
    public FeatureResponse deleteFeature(Long id) {
        return changeStatus(id, new FeatureStatusRequest(DISABLED));
    }

    private QuestionnaireFeature requireExistingFeature(Long id) {
        validateId(id);
        QuestionnaireFeature feature = featureMapper.selectById(id);
        if (feature == null) {
            throw notFound(id);
        }
        return feature;
    }

    private FeatureResponse reloadFeature(Long id) {
        QuestionnaireFeature feature = requireExistingFeature(id);
        return FeatureResponse.from(feature);
    }

    private void validateId(Long id) {
        if (id == null || id <= 0) {
            throw invalid("特性ID必须为正整数");
        }
    }

    private String normalizeFeatureCode(String value) {
        String normalized = normalizeText(value);
        if (normalized == null) {
            throw invalid("特性编码不能为空");
        }
        if (normalized.length() > MAX_FEATURE_CODE_LENGTH
                || !FEATURE_CODE_PATTERN.matcher(normalized).matches()) {
            throw invalid("特性编码只能包含字母、数字、下划线、点和短横线，长度不能超过64");
        }
        return normalized;
    }

    private String normalizeFeatureName(String value) {
        String normalized = normalizeText(value);
        if (normalized == null) {
            throw invalid("特性名称不能为空");
        }
        if (normalized.length() > MAX_FEATURE_NAME_LENGTH) {
            throw invalid("特性名称长度不能超过128");
        }
        return normalized;
    }

    private int normalizeSortNo(Integer sortNo) {
        if (sortNo == null) {
            return 0;
        }
        if (sortNo < 0) {
            throw invalid("排序号不能为负数");
        }
        return sortNo;
    }

    private int normalizeStatus(Integer status, Integer defaultStatus) {
        Integer value = status == null ? defaultStatus : status;
        if (value == null) {
            throw invalid("状态不能为空");
        }
        if (value != ENABLED && value != DISABLED) {
            throw invalid("状态只支持0或1");
        }
        return value;
    }

    private String normalizeText(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private QuestionnaireFeatureException invalid(String message) {
        return new QuestionnaireFeatureException(
                INVALID_CODE,
                HttpStatus.BAD_REQUEST,
                message);
    }

    private QuestionnaireFeatureException notFound(Long id) {
        return new QuestionnaireFeatureException(
                NOT_FOUND_CODE,
                HttpStatus.NOT_FOUND,
                "特性不存在：" + id);
    }
}
