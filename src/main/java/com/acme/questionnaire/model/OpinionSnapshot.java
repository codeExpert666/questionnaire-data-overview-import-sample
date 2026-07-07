package com.acme.questionnaire.model;

import lombok.Builder;
import lombok.Value;

/**
 * Excel 中一行观点明细的解析快照。
 *
 * <p>同一问卷的多条观点按读取顺序追加到 AnswerAggregate，写库时再生成从 1 开始的 opinion_seq。</p>
 */
@Value
@Builder
public class OpinionSnapshot {
    /** Excel 原始行号，用于错误定位和排查导入数据。 */
    int rowNumber;
    Sentiment sentiment;
    /** 可为空；保存 Excel 固定列“特性分类名称”的自由文本。 */
    String featureCategoryName;
    String feedbackContent1;
    String feedbackContent2;
}
