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
import java.util.regex.Pattern;

/**
 * pq_product 产品型号字典的业务规则入口。
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
     */
    public List<ProductResponse> listProducts() {
        return productMapper.selectAllProducts().stream()
                .map(ProductResponse::from)
                .toList();
    }

    /**
     * 创建产品型号。
     *
     * <p>productCode 是导入文件中的稳定匹配键，创建后不提供修改入口。</p>
     */
    @Transactional(rollbackFor = Exception.class)
    public ProductResponse createProduct(ProductCreateRequest request) {
        String productCode = normalizeProductCode(request == null ? null : request.productCode());
        String productModel = normalizeProductModel(request == null ? null : request.productModel());
        int status = normalizeStatus(request == null ? null : request.status(), ENABLED);

        if (productMapper.existsByProductCode(productCode)) {
            throw invalid("产品编码已存在：" + productCode);
        }

        QuestionnaireProduct product = new QuestionnaireProduct();
        product.setProductCode(productCode);
        product.setProductModel(productModel);
        product.setStatus(status);
        try {
            productMapper.insertProduct(product);
        } catch (DuplicateKeyException ex) {
            throw invalid("产品编码已存在：" + productCode);
        }

        cacheVersionService.increaseAfterCommit();
        return reloadProduct(product.getId());
    }

    /**
     * 更新产品型号展示名。
     */
    @Transactional(rollbackFor = Exception.class)
    public ProductResponse updateProduct(Long id, ProductUpdateRequest request) {
        requireExistingProduct(id);

        QuestionnaireProduct product = new QuestionnaireProduct();
        product.setId(id);
        product.setProductModel(normalizeProductModel(
                request == null ? null : request.productModel()));

        if (productMapper.updateProduct(product) == 0) {
            throw notFound(id);
        }

        cacheVersionService.increaseAfterCommit();
        return reloadProduct(id);
    }

    /**
     * 修改产品启停状态。
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
