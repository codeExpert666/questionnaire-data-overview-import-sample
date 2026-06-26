package com.acme.questionnaire.service;

import com.acme.questionnaire.dto.ProductCreateRequest;
import com.acme.questionnaire.dto.ProductResponse;
import com.acme.questionnaire.dto.ProductStatusRequest;
import com.acme.questionnaire.dto.ProductUpdateRequest;
import com.acme.questionnaire.exception.QuestionnaireProductException;
import com.acme.questionnaire.mapper.ProductMapper;
import com.acme.questionnaire.model.QuestionnaireProduct;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * pq_product 产品型号字典的业务规则入口。
 *
 * <p>该服务承载产品主数据的写入规范：product_code 是 Excel 导入时的稳定匹配键，
 * 创建后不提供修改入口；product_model 是面向用户的展示名，允许后续更名；status
 * 控制产品是否进入新模板和新导入校验，历史答卷仍通过 product_id 保留原引用。</p>
 *
 * <p>任一会影响模板或导入校验结果的变更，都必须在事务提交后递增缓存版本，使前端或
 * 下游缓存可以感知产品字典已经变化。这里的 Redis 版本号只是失效信号，本服务仍直接
 * 从数据库读取产品字典，不在 Redis 中缓存产品明细。</p>
 */
@Service
@RequiredArgsConstructor
public class QuestionnaireProductService {
    private static final String INVALID_CODE = "QUESTIONNAIRE_PRODUCT_INVALID";
    private static final String NOT_FOUND_CODE = "QUESTIONNAIRE_PRODUCT_NOT_FOUND";
    private static final int ENABLED = 1;
    private static final int DISABLED = 0;
    private static final int MAX_PRODUCT_CODE_LENGTH = 64;
    private static final int MAX_PRODUCT_MODEL_LENGTH = 128;
    private static final Pattern PRODUCT_CODE_PATTERN =
            Pattern.compile("^[A-Za-z0-9_.-]{1,64}$");

    private final ProductMapper productMapper;
    private final QuestionnaireCacheVersionService cacheVersionService;

    /**
     * 查询完整产品字典，包含停用产品。
     *
     * <p>维护页面需要看到停用项，才能重新启用或排查历史答卷引用；模板下载和导入校验
     * 不调用该方法，它们只读取启用产品。</p>
     */
    public List<ProductResponse> listProducts() {
        return productMapper.selectAllProducts().stream()
                .map(ProductResponse::from)
                .toList();
    }

    /**
     * 查询启用产品。
     *
     * <p>产品特性配置页的产品筛选框只应展示 status=1 的产品，避免用户继续配置已停用产品。</p>
     */
    public List<ProductResponse> listEnabledProducts() {
        return productMapper.selectEnabledProducts().stream()
                .map(ProductResponse::from)
                .toList();
    }

    /**
     * 创建产品型号。
     *
     * <p>productCode 为空时自动生成 P{id}；非空时会先去除首尾空白，再按字母、数字、
     * 下划线、点和短横线校验。显式编码和产品名称都由应用层先查重，数据库唯一索引再
     * 兜底并发创建。status 为空时按启用处理。</p>
     */
    @Transactional(rollbackFor = Exception.class)
    public ProductResponse createProduct(ProductCreateRequest request) {
        String requestedProductCode = normalizeText(request == null ? null : request.productCode());
        boolean autoGenerateCode = requestedProductCode == null;
        String productCode = autoGenerateCode
                ? temporaryProductCode()
                : normalizeProductCode(requestedProductCode);
        String productModel = normalizeProductModel(request == null ? null : request.productModel());
        int status = normalizeStatus(request == null ? null : request.status(), ENABLED);

        if (!autoGenerateCode && productMapper.existsByProductCode(productCode)) {
            throw invalid("产品编码已存在：" + productCode);
        }
        if (productMapper.existsByProductModel(productModel)) {
            throw invalid("产品名称已存在：" + productModel);
        }

        QuestionnaireProduct product = new QuestionnaireProduct();
        product.setProductCode(productCode);
        product.setProductModel(productModel);
        product.setStatus(status);
        try {
            productMapper.insertProduct(product);
        } catch (DuplicateKeyException ex) {
            throw invalid("产品编码或产品名称已存在");
        }

        if (autoGenerateCode) {
            productCode = buildProductCode(product.getId());
            QuestionnaireProduct codeUpdate = new QuestionnaireProduct();
            codeUpdate.setId(product.getId());
            codeUpdate.setProductCode(productCode);
            try {
                if (productMapper.updateProductCode(codeUpdate) == 0) {
                    throw notFound(product.getId());
                }
            } catch (DuplicateKeyException ex) {
                throw invalid("产品编码已存在：" + productCode);
            }
        }

        cacheVersionService.increaseAfterCommit();
        return reloadProduct(product.getId());
    }

    /**
     * 更新产品型号展示名。
     *
     * <p>产品编码不可在此修改，避免旧模板、历史答卷和外部配置中使用的稳定编码失效。
     * 型号变更后，用户需要重新下载模板，以获得产品字典中的最新展示名。</p>
     */
    @Transactional(rollbackFor = Exception.class)
    public ProductResponse updateProduct(Long id, ProductUpdateRequest request) {
        requireExistingProduct(id);
        String productModel = normalizeProductModel(
                request == null ? null : request.productModel());
        if (productMapper.existsByProductModelExceptId(productModel, id)) {
            throw invalid("产品名称已存在：" + productModel);
        }

        QuestionnaireProduct product = new QuestionnaireProduct();
        product.setId(id);
        product.setProductModel(productModel);

        try {
            if (productMapper.updateProduct(product) == 0) {
                throw notFound(id);
            }
        } catch (DuplicateKeyException ex) {
            throw invalid("产品名称已存在：" + productModel);
        }

        cacheVersionService.increaseAfterCommit();
        return reloadProduct(id);
    }

    /**
     * 修改产品启停状态。
     *
     * <p>启用产品会进入新模板的“产品字典”工作表，并允许新导入引用；停用产品从模板和
     * 新导入校验中移除，但不会删除 pq_answer 中已经存在的 product_id 外键。</p>
     */
    @Transactional(rollbackFor = Exception.class)
    public ProductResponse changeStatus(Long id, ProductStatusRequest request) {
        QuestionnaireProduct existing = requireExistingProduct(id);
        int status = normalizeStatus(request == null ? null : request.status(), null);
        if (existing.getStatus() != null && existing.getStatus() == status) {
            return ProductResponse.from(existing);
        }

        QuestionnaireProduct product = new QuestionnaireProduct();
        product.setId(id);
        product.setStatus(status);
        if (productMapper.updateProductStatus(product) == 0) {
            throw notFound(id);
        }

        cacheVersionService.increaseAfterCommit();
        return reloadProduct(id);
    }

    /**
     * 软删除产品，保留历史答卷外键引用。
     *
     * <p>删除语义等价于 status=0；不执行物理删除，避免破坏 pq_answer 和
     * pq_product_feature 的历史关联。</p>
     */
    @Transactional(rollbackFor = Exception.class)
    public ProductResponse deleteProduct(Long id) {
        return changeStatus(id, new ProductStatusRequest(DISABLED));
    }

    private QuestionnaireProduct requireExistingProduct(Long id) {
        validateId(id);
        QuestionnaireProduct product = productMapper.selectById(id);
        if (product == null) {
            throw notFound(id);
        }
        return product;
    }

    private ProductResponse reloadProduct(Long id) {
        QuestionnaireProduct product = requireExistingProduct(id);
        return ProductResponse.from(product);
    }

    private void validateId(Long id) {
        if (id == null || id <= 0) {
            throw invalid("产品ID必须为正整数");
        }
    }

    private String normalizeProductCode(String value) {
        String normalized = normalizeText(value);
        if (normalized == null) {
            throw invalid("产品编码不能为空");
        }
        if (normalized.length() > MAX_PRODUCT_CODE_LENGTH
                || !PRODUCT_CODE_PATTERN.matcher(normalized).matches()) {
            throw invalid("产品编码只能包含字母、数字、下划线、点和短横线，长度不能超过64");
        }
        return normalized;
    }

    private String temporaryProductCode() {
        return "__AUTO_P_" + UUID.randomUUID().toString().replace("-", "");
    }

    private String buildProductCode(Long id) {
        if (id == null || id <= 0) {
            throw invalid("产品ID必须为正整数");
        }
        return "P" + id;
    }

    private String normalizeProductModel(String value) {
        String normalized = normalizeText(value);
        if (normalized == null) {
            throw invalid("产品型号不能为空");
        }
        if (normalized.length() > MAX_PRODUCT_MODEL_LENGTH) {
            throw invalid("产品型号长度不能超过128");
        }
        return normalized;
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

    private QuestionnaireProductException invalid(String message) {
        return new QuestionnaireProductException(
                INVALID_CODE,
                HttpStatus.BAD_REQUEST,
                message);
    }

    private QuestionnaireProductException notFound(Long id) {
        return new QuestionnaireProductException(
                NOT_FOUND_CODE,
                HttpStatus.NOT_FOUND,
                "产品不存在：" + id);
    }
}
