package com.acme.questionnaire.mapper;

import com.acme.questionnaire.model.QuestionnaireProduct;
import com.acme.questionnaire.ref.ProductRef;

import java.util.List;

public interface ProductMapper {
    /**
     * 查询启用产品，供模板下载和导入校验使用。
     */
    List<ProductRef> selectEnabledProducts();

    /**
     * 查询完整产品字典，包含停用产品。
     */
    List<QuestionnaireProduct> selectAllProducts();

    QuestionnaireProduct selectById(Long id);

    boolean existsByProductCode(String productCode);

    int insertProduct(QuestionnaireProduct product);

    int updateProduct(QuestionnaireProduct product);

    int updateProductStatus(QuestionnaireProduct product);
}
