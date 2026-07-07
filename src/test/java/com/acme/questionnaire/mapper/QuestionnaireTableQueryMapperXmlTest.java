package com.acme.questionnaire.mapper;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class QuestionnaireTableQueryMapperXmlTest {
    @Test
    void mapperXmlOnlyUsesServiceGeneratedSqlTextForOrderAndJoinAliases() throws IOException {
        String xml = readXml();

        assertThat(xml).contains("<sql id=\"CommonAnswerProductFilters\">");
        assertThat(xml).contains("<sql id=\"ScoreExistsFilters\">");
        assertThat(xml).contains("${order.expression}");
        assertThat(xml).contains("${sort.alias}");
        assertThat(xml).doesNotContain("${criteria");
        assertThat(xml).doesNotContain("${filters");
        assertThat(xml).doesNotContain("${sort.field}");
        assertThat(xml).doesNotContain("${sort.direction}");
    }

    @Test
    void dataOverviewUsesOpinionFeatureCategoryFilterNotScoreExistsFilter() throws IOException {
        String xml = readXml();
        String countDataOverview = section(xml, "<select id=\"countDataOverview\"", "</select>");
        String selectDataOverviewRows = section(xml, "<select id=\"selectDataOverviewRows\"", "</select>");

        assertThat(countDataOverview)
                .contains("<include refid=\"FeatureScoreFilters\"/>")
                .contains("<include refid=\"OpinionFeatureCategoryFilter\"/>")
                .doesNotContain("<include refid=\"ScoreExistsFilters\"/>");
        assertThat(selectDataOverviewRows)
                .contains("<include refid=\"FeatureScoreFilters\"/>")
                .contains("<include refid=\"OpinionFeatureCategoryFilter\"/>")
                .doesNotContain("<include refid=\"ScoreExistsFilters\"/>")
                .doesNotContain("o.feature_id");
    }

    private String readXml() throws IOException {
        try (InputStream inputStream = getClass().getResourceAsStream(
                "/mapper/QuestionnaireTableQueryMapper.xml")) {
            assertThat(inputStream).isNotNull();
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private String section(String text, String startMarker, String endMarker) {
        int start = text.indexOf(startMarker);
        assertThat(start).isNotNegative();
        int end = text.indexOf(endMarker, start);
        assertThat(end).isNotNegative();
        return text.substring(start, end + endMarker.length());
    }
}
