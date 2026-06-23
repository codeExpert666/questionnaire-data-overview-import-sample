package com.acme.questionnaire.service;

import com.acme.questionnaire.config.QuestionnaireImportProperties;
import com.acme.questionnaire.model.UserCategory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RecommendCategoryResolver {
    private final QuestionnaireImportProperties properties;

    public UserCategory resolve(Integer score) {
        if (score == null) {
            return UserCategory.UNKNOWN;
        }
        if (score <= properties.getDetractorMaxScore()) {
            return UserCategory.DETRACTOR;
        }
        if (score <= properties.getPassiveMaxScore()) {
            return UserCategory.PASSIVE;
        }
        return UserCategory.PROMOTER;
    }
}
