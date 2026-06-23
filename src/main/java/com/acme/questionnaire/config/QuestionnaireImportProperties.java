package com.acme.questionnaire.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "questionnaire.import")
public class QuestionnaireImportProperties {
    private long maxFileSizeBytes = 20L * 1024 * 1024;
    private int maxDataRows = 100_000;
    private int maxErrors = 200;
    private int answerBatchSize = 100;
    private int detractorMaxScore = 6;
    private int passiveMaxScore = 8;
}
