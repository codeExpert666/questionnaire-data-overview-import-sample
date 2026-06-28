package com.acme.questionnaire.mapper;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class QuestionnaireScoreAnalyticsMapperXmlTest {
    @Test
    void mapperXmlUsesOnlyServiceGeneratedBucketSqlText() throws IOException {
        String xml = readXml();

        assertThat(xml).contains("<sql id=\"AnalyticsFilters\">");
        assertThat(xml).contains("<foreach collection=\"criteria.productModels\"");
        assertThat(xml).contains("${bucketExpression}");
        assertThat(xml).doesNotContain("${criteria");
        assertThat(xml).doesNotContain("${request");
        assertThat(xml).doesNotContain("${sort");
    }

    private String readXml() throws IOException {
        try (InputStream inputStream = getClass().getResourceAsStream(
                "/mapper/QuestionnaireScoreAnalyticsMapper.xml")) {
            assertThat(inputStream).isNotNull();
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
