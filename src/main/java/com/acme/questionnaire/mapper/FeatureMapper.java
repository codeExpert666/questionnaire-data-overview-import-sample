package com.acme.questionnaire.mapper;

import com.acme.questionnaire.ref.FeatureRef;

import java.util.List;

public interface FeatureMapper {
    List<FeatureRef> selectEnabledFeatures();
}
