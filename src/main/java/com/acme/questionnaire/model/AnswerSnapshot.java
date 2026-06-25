package com.acme.questionnaire.model;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Excel 中解析出的问卷级数据快照。
 *
 * <p>该对象不是数据库实体，而是导入监听器和写库组件之间的不可变传输对象。productCode 和
 * productModel 只用于校验和错误提示；真正写入 pq_answer 的产品引用是 productId。</p>
 */
@Value
@Builder
public class AnswerSnapshot {
    /** 该问卷首次出现的 Excel 行号，用于重复行字段冲突提示。 */
    int firstRowNumber;
    String questionnaireId;
    Long productId;
    String productCode;
    String productModel;
    LocalDateTime answerTime;
    String romVersion;
    String appVersion;
    String feedbackText;
    String scoreReason;
    Integer recommendScore;
    UserCategory userCategory;
    /** 已通过产品适用性校验的非空特性评分，key 为 pq_feature.id。 */
    Map<Long, Integer> featureScores;
}
