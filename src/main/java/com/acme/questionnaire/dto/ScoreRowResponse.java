package com.acme.questionnaire.dto;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 评分页单行响应。
 *
 * <p>一行对应 pq_answer 中的一份问卷答卷，固定字段来自答卷和产品表。动态特性评分不声明为
 * Java 固定属性，而是写入 featureScores 后通过 JsonAnyGetter 扁平化输出，保证 JSON 结构
 * 与 columns 中的 featureScore:{featureId} 动态列 key 完全一致。</p>
 */
@Getter
@Setter
public class ScoreRowResponse {
    /** pq_answer.id，供前端需要定位原始答卷时使用。 */
    private Long answerId;
    /** Excel 中的问卷 ID，同一 source_system 下是导入覆盖的业务键。 */
    private String questionnaireId;
    /** 产品展示型号，来自 pq_product.product_model。 */
    private String productModel;
    /** 稳定产品编码，来自 pq_product.product_code。 */
    private String productCode;
    /** 答卷提交时间。 */
    private LocalDateTime answerTime;
    /** 答卷对应的 ROM 版本。 */
    private String romVersion;
    /** 答卷对应的 App 版本。 */
    private String appVersion;
    /** 推荐意愿评分，取值范围由导入校验控制为 1 到 10。 */
    private Integer recommendScore;
    /** 用户归类展示名，由 pq_answer.user_category 枚举编码转换得到。 */
    private String userCategory;

    /** 动态评分列缓存；LinkedHashMap 保持按启用特性顺序写入后的稳定 JSON 顺序。 */
    @JsonIgnore
    private final Map<String, Integer> featureScores = new LinkedHashMap<>();

    /**
     * 写入一个动态特性评分。
     *
     * <p>调用方传入数据库主键 featureId，本方法统一生成响应字段名 featureScore:{featureId}。
     * 未写入的特性不会出现在当前行 JSON 中，前端可根据 columns 中的列定义按空值展示。</p>
     */
    public void putFeatureScore(Long featureId, Integer score) {
        featureScores.put("featureScore:" + featureId, score);
    }

    /**
     * 将动态评分字段扁平输出到行对象顶层。
     *
     * <p>示例：featureId=7、score=8 时，JSON 行中输出 "featureScore:7": 8，
     * 该 key 同时也是前端请求动态排序时使用的 field。</p>
     */
    @JsonAnyGetter
    public Map<String, Integer> getFlattenedFeatureScores() {
        return featureScores;
    }
}
