package com.acme.questionnaire.mapper;

import com.acme.questionnaire.model.QuestionnaireProduct;
import com.acme.questionnaire.ref.ProductRef;

import java.util.List;

/**
 * pq_product 的 MyBatis 访问接口。
 *
 * <p>写入侧服务负责参数规范化和业务异常转换；Mapper 只表达数据库读写语义。
 * selectEnabledProducts 专供模板下载和导入校验，selectAllProducts 专供维护页面。</p>
 */
public interface ProductMapper {
    /**
     * 查询启用产品，供模板下载和导入校验使用。
     *
     * <p>只返回 status=1 的产品，结果映射为轻量 ProductRef，避免把维护字段暴露给导入流程。</p>
     */
    List<ProductRef> selectEnabledProducts();

    /**
     * 查询完整产品字典，包含停用产品。
     *
     * <p>维护页面使用该列表展示、编辑、启停和软删除产品。</p>
     */
    List<QuestionnaireProduct> selectAllProducts();

    /**
     * 按主键查询产品。
     *
     * <p>产品维护操作先用该方法确认目标存在，再执行更新，保证不存在时返回统一业务错误。</p>
     */
    QuestionnaireProduct selectById(Long id);

    /**
     * 检查稳定产品编码是否已存在。
     *
     * <p>创建产品时先做友好错误提示；并发场景仍依赖数据库唯一索引兜底。</p>
     */
    boolean existsByProductCode(String productCode);

    /**
     * 新增产品字典项。
     *
     * <p>使用数据库自增主键回填 id，便于创建后重新查询完整响应。</p>
     */
    int insertProduct(QuestionnaireProduct product);

    /**
     * 更新创建时自动生成的稳定产品编码。
     *
     * <p>仅供服务层在拿到自增 id 后把临时编码替换为 P{id}；普通更新接口不调用。</p>
     */
    int updateProductCode(QuestionnaireProduct product);

    /**
     * 更新产品可变展示字段。
     *
     * <p>只更新 product_model，不允许通过 Mapper 调用改写 product_code。</p>
     */
    int updateProduct(QuestionnaireProduct product);

    /**
     * 更新启停状态。
     *
     * <p>status=0 同时承担软删除语义，不能物理删除 pq_product 记录。</p>
     */
    int updateProductStatus(QuestionnaireProduct product);
}
