package com.acme.questionnaire.model;

import lombok.Value;

/**
 * MyBatis ORDER BY 子句中的单个排序项。
 *
 * <p>expression 不是前端原始入参，而是 QuestionnaireTableQueryService 根据排序白名单或
 * 动态评分排序别名生成的 SQL 片段；direction 只允许 ASC 或 DESC。XML 使用 ${} 输出时
 * 依赖该约束，禁止绕过服务层直接构造。</p>
 */
@Value
public class TableOrderClause {
    /** 受控 SQL 排序表达式，例如 a.answer_time 或 sort_score_0.score。 */
    String expression;
    /** 排序方向，只能是 ASC 或 DESC。 */
    String direction;
}
