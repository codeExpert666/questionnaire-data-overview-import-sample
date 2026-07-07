package com.acme.questionnaire.mapper;

import com.acme.questionnaire.model.QuestionnaireFeature;
import com.acme.questionnaire.ref.FeatureRef;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * pq_feature 字典 Mapper。
 *
 * <p>这里区分两个读取场景：模板和导入只读取启用特性；后台维护页读取全部特性。
 * 写入方法只维护字典本身，不维护产品与特性的适用关系。</p>
 */
public interface FeatureMapper {
    /**
     * 查询启用特性。
     *
     * <p>结果用于生成 Excel 动态评分列、特性字典工作表和导入表头校验。
     * 顺序必须与模板列顺序稳定一致。</p>
     */
    List<FeatureRef> selectEnabledFeatures();

    /**
     * 查询完整字典。
     *
     * <p>包含停用特性，供维护接口展示和编辑。</p>
     */
    List<QuestionnaireFeature> selectAllFeatures();

    /**
     * 按主键查询特性。
     *
     * <p>启用和停用记录都可查到，用于维护接口更新、停用和软删除。</p>
     */
    QuestionnaireFeature selectById(Long id);

    /**
     * 判断稳定编码是否已存在。
     *
     * <p>调用方仍需依赖 uk_feature_code 兜底处理并发创建。</p>
     */
    boolean existsByFeatureCode(String featureCode);

    /**
     * 判断展示名称是否已存在。
     *
     * <p>Excel 动态评分列表头依赖 feature_name 解析，名称必须全表唯一。</p>
     */
    boolean existsByFeatureName(String featureName);

    /**
     * 更新时判断除当前特性外是否已有相同展示名称。
     */
    boolean existsByFeatureNameExceptId(@Param("featureName") String featureName,
                                        @Param("id") Long id);

    /**
     * 新建特性字典项。
     *
     * <p>使用数据库自增主键回填 id，供创建后回读完整记录。</p>
     */
    int insertFeature(QuestionnaireFeature feature);

    /**
     * 更新创建时自动生成的稳定特性编码。
     *
     * <p>仅供服务层在拿到自增 id 后把临时编码替换为 F{id}；普通更新接口不调用。</p>
     */
    int updateFeatureCode(QuestionnaireFeature feature);

    /**
     * 更新展示名称和排序号。
     *
     * <p>不更新 feature_code，保持模板和历史数据中的编码稳定。</p>
     */
    int updateFeature(QuestionnaireFeature feature);

    /**
     * 更新启停状态。
     *
     * <p>停用不会删除记录，历史评分和观点仍可通过外键引用该特性。</p>
     */
    int updateFeatureStatus(QuestionnaireFeature feature);
}
