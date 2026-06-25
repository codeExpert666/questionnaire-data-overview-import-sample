package com.acme.questionnaire.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "questionnaire.import")
public class QuestionnaireImportProperties {
    /** 上传文件大小的业务上限；Controller 之前还会经过 Spring multipart 限制。 */
    private long maxFileSizeBytes = 20L * 1024 * 1024;
    /** 单个工作簿最多允许读取的数据行数，不包含表头和被 EasyExcel 忽略的空行。 */
    private int maxDataRows = 100_000;
    /** 最多收集并返回的行级错误数量；达到上限后监听器会提前中断解析。 */
    private int maxErrors = 200;
    /** 问卷聚合批量写库阈值；写库仍在导入事务内，失败会整体回滚。 */
    private int answerBatchSize = 100;
    /** 推荐意愿小于等于该值时自动归类为贬损用户。 */
    private int detractorMaxScore = 6;
    /** 推荐意愿大于 detractorMaxScore 且小于等于该值时自动归类为被动用户。 */
    private int passiveMaxScore = 8;
}
