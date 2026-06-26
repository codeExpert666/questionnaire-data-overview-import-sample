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
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * pq_feature 字典的业务规则入口。
 *
 * <p>特性编码 feature_code 是后台和 API 使用的稳定标识：创建时写入，后续不允许修改。
 * 展示名称和排序号可以维护，但名称或排序变更会导致旧模板的固定分类名称或动态评分列表头
 * 校验失败，提示用户重新下载模板。</p>
 *
 * <p>status 使用 1/0 表示启用/停用。停用不会删除记录，目的是保持历史评分
 * pq_answer_feature_score 和观点归类 pq_opinion.feature_id 的外键引用有效。
 * 每次字典变更在事务提交后递增缓存版本，通知外部缓存或前端刷新字典和模板。这里的
 * Redis 版本号只是失效信号，本服务仍直接从数据库读取特性字典，不在 Redis 中缓存
 * 特性明细或模板列定义。</p>
 */
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

    /**
     * 查询完整特性字典。
     *
     * <p>返回启用和停用数据，排序规则由 Mapper 固定为启用优先、sort_no 升序、id 升序。</p>
     */
    public List<FeatureResponse> listFeatures() {
        return featureMapper.selectAllFeatures().stream()
                .map(FeatureResponse::from)
                .toList();
    }

    /**
     * 创建 pq_feature 记录。
     *
     * <p>默认 status 为启用，sortNo 为空时按 0 处理。featureCode 为空时自动生成
     * F{id}；非空时会去除首尾空白、校验长度和字符集。编码和名称都在应用层与数据库唯一
     * 约束两层防重。</p>
     *
     * @throws QuestionnaireFeatureException 当编码、名称、排序号、状态不合法或编码重复时抛出
     */
    @Transactional(rollbackFor = Exception.class)
    public FeatureResponse createFeature(FeatureCreateRequest request) {
        String requestedFeatureCode = normalizeText(request == null ? null : request.featureCode());
        boolean autoGenerateCode = requestedFeatureCode == null;
        String featureCode = autoGenerateCode
                ? temporaryFeatureCode()
                : normalizeFeatureCode(requestedFeatureCode);
        String featureName = normalizeFeatureName(request == null ? null : request.featureName());
        int sortNo = normalizeSortNo(request == null ? null : request.sortNo());
        int status = normalizeStatus(request == null ? null : request.status(), ENABLED);

        if (!autoGenerateCode && featureMapper.existsByFeatureCode(featureCode)) {
            throw invalid("特性编码已存在：" + featureCode);
        }
        if (featureMapper.existsByFeatureName(featureName)) {
            throw invalid("特性名称已存在：" + featureName);
        }

        QuestionnaireFeature feature = new QuestionnaireFeature();
        feature.setFeatureCode(featureCode);
        feature.setFeatureName(featureName);
        feature.setSortNo(sortNo);
        feature.setStatus(status);
        try {
            featureMapper.insertFeature(feature);
        } catch (DuplicateKeyException ex) {
            throw invalid("特性编码或特性名称已存在");
        }

        if (autoGenerateCode) {
            featureCode = buildFeatureCode(feature.getId());
            QuestionnaireFeature codeUpdate = new QuestionnaireFeature();
            codeUpdate.setId(feature.getId());
            codeUpdate.setFeatureCode(featureCode);
            try {
                if (featureMapper.updateFeatureCode(codeUpdate) == 0) {
                    throw notFound(feature.getId());
                }
            } catch (DuplicateKeyException ex) {
                throw invalid("特性编码已存在：" + featureCode);
            }
        }

        cacheVersionService.increaseAfterCommit();
        return reloadFeature(feature.getId());
    }

    /**
     * 更新可变展示属性。
     *
     * <p>不允许修改 featureCode；如果需要废弃旧编码，应停用旧特性并新建编码，
     * 这样历史数据仍能指向原 pq_feature.id。</p>
     */
    @Transactional(rollbackFor = Exception.class)
    public FeatureResponse updateFeature(Long id, FeatureUpdateRequest request) {
        requireExistingFeature(id);
        String featureName = normalizeFeatureName(request == null ? null : request.featureName());
        if (featureMapper.existsByFeatureNameExceptId(featureName, id)) {
            throw invalid("特性名称已存在：" + featureName);
        }

        QuestionnaireFeature feature = new QuestionnaireFeature();
        feature.setId(id);
        feature.setFeatureName(featureName);
        feature.setSortNo(normalizeSortNo(request == null ? null : request.sortNo()));

        try {
            if (featureMapper.updateFeature(feature) == 0) {
                throw notFound(id);
            }
        } catch (DuplicateKeyException ex) {
            throw invalid("特性名称已存在：" + featureName);
        }

        cacheVersionService.increaseAfterCommit();
        return reloadFeature(id);
    }

    /**
     * 修改特性启停状态。
     *
     * <p>启用状态决定模板下载、模板表头校验、特性分类名称校验和评分列解析是否接受该特性。
     * 状态未变化时直接返回当前记录，避免产生无意义的缓存版本递增。</p>
     */
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

    /**
     * 软删除特性。
     *
     * <p>当前实现复用状态修改逻辑，将 status 固定改为 0。</p>
     */
    @Transactional(rollbackFor = Exception.class)
    public FeatureResponse deleteFeature(Long id) {
        return changeStatus(id, new FeatureStatusRequest(DISABLED));
    }

    /**
     * 读取并确认特性存在。
     *
     * <p>该检查同时约束 id 必须是正整数，避免 Mapper 接收无效主键。</p>
     */
    private QuestionnaireFeature requireExistingFeature(Long id) {
        validateId(id);
        QuestionnaireFeature feature = featureMapper.selectById(id);
        if (feature == null) {
            throw notFound(id);
        }
        return feature;
    }

    /**
     * 重新读取数据库中的记录。
     *
     * <p>insert/update 后统一从数据库回读，确保返回 createdAt、updatedAt 和默认值的最终状态。</p>
     */
    private FeatureResponse reloadFeature(Long id) {
        QuestionnaireFeature feature = requireExistingFeature(id);
        return FeatureResponse.from(feature);
    }

    private void validateId(Long id) {
        if (id == null || id <= 0) {
            throw invalid("特性ID必须为正整数");
        }
    }

    /**
     * 规范化特性编码。
     *
     * <p>允许字母、数字、下划线、点和短横线，长度最多 64。该编码保留给后台和 API 使用，
     * 不再作为 Excel 观点分类的用户填写值。</p>
     */
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

    private String temporaryFeatureCode() {
        return "__AUTO_F_" + UUID.randomUUID().toString().replace("-", "");
    }

    private String buildFeatureCode(Long id) {
        if (id == null || id <= 0) {
            throw invalid("特性ID必须为正整数");
        }
        return "F" + id;
    }

    /**
     * 规范化特性名称。
     *
     * <p>名称是模板动态列的主体部分；导入时会与当前名称精确匹配，用于发现旧模板。</p>
     */
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

    /**
     * 规范化排序号。
     *
     * <p>sort_no 只影响模板列顺序和维护列表顺序，不表达产品适用关系。</p>
     */
    private int normalizeSortNo(Integer sortNo) {
        if (sortNo == null) {
            return 0;
        }
        if (sortNo < 0) {
            throw invalid("排序号不能为负数");
        }
        return sortNo;
    }

    /**
     * 规范化状态值。
     *
     * <p>只接受 1 或 0，分别对应启用和停用；创建接口使用启用作为默认值。</p>
     */
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
