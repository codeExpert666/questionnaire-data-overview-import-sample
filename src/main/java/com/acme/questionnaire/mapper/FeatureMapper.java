package com.acme.questionnaire.mapper;

import com.acme.questionnaire.model.QuestionnaireFeature;
import com.acme.questionnaire.ref.FeatureRef;

import java.util.List;

public interface FeatureMapper {
    List<FeatureRef> selectEnabledFeatures();

    List<QuestionnaireFeature> selectAllFeatures();

    QuestionnaireFeature selectById(Long id);

    boolean existsByFeatureCode(String featureCode);

    int insertFeature(QuestionnaireFeature feature);

    int updateFeature(QuestionnaireFeature feature);

    int updateFeatureStatus(QuestionnaireFeature feature);
}
