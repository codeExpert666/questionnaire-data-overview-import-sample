package com.acme.questionnaire.db;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class SchemaReferenceTest {
    @Test
    void productFeatureTableDoesNotContainUnusedConfigurationColumns() throws Exception {
        String schema = Files.readString(Path.of("db/schema-reference.sql"));
        String productFeatureTable = schema.substring(
                schema.indexOf("CREATE TABLE IF NOT EXISTS pq_product_feature"),
                schema.indexOf("CREATE TABLE IF NOT EXISTS pq_answer"));

        assertThat(productFeatureTable)
                .doesNotContain("display_order")
                .doesNotContain("required_flag");
    }
}
