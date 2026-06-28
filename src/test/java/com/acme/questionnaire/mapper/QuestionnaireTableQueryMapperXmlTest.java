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

    private String readXml() throws IOException {
        try (InputStream inputStream = getClass().getResourceAsStream(
                "/mapper/QuestionnaireTableQueryMapper.xml")) {
            assertThat(inputStream).isNotNull();
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
