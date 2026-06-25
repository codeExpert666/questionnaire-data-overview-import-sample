package com.acme.questionnaire.model;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

/**
 * 一个问卷 ID 对应的导入聚合。
 *
 * <p>Excel 中同一 questionnaire_id 可以有多行观点，但问卷级字段和特性评分必须一致。
 * 监听器把这些连续行合并成一个聚合后，再交给写库组件按问卷整体覆盖。</p>
 */
@Getter
public class AnswerAggregate {
    /** 问卷级快照，来自该问卷第一条有效行。 */
    private final AnswerSnapshot answer;
    /** 观点明细列表，顺序保持 Excel 中同一问卷连续行的读取顺序。 */
    private final List<OpinionSnapshot> opinions = new ArrayList<>();

    public AnswerAggregate(AnswerSnapshot answer) {
        this.answer = answer;
    }

    public void addOpinion(OpinionSnapshot opinion) {
        opinions.add(opinion);
    }
}
