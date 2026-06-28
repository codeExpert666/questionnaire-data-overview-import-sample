package com.acme.questionnaire.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
@EnableConfigurationProperties(QuestionnaireImportProperties.class)
public class QuestionnaireImportConfiguration {
    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }
}
