package com.acme.questionnaire;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@MapperScan("com.acme.questionnaire.mapper")
@SpringBootApplication
public class QuestionnaireImportApplication {
    public static void main(String[] args) {
        SpringApplication.run(QuestionnaireImportApplication.class, args);
    }
}
