package com.acme.questionnaire.excel;

import com.acme.questionnaire.ref.FeatureRef;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class QuestionnaireExcelHeadersTest {

    @Test
    void featureScoreHeaderUsesFeatureNamePlusExperience() {
        FeatureRef feature = new FeatureRef();
        feature.setFeatureCode("AUDIO");
        feature.setFeatureName("音质音效");

        String header = QuestionnaireExcelHeaders.featureScoreHeader(feature);

        assertThat(header).isEqualTo("音质音效体验");
    }

    @Test
    void parseFeatureScoreHeaderReadsFeatureNameFromExperienceHeader() {
        QuestionnaireExcelHeaders.ParsedFeatureHeader parsed =
                QuestionnaireExcelHeaders.parseFeatureScoreHeader("音质音效体验");

        assertThat(parsed.featureName()).isEqualTo("音质音效");
    }
}
