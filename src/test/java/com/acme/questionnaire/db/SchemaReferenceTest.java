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

    @Test
    void productAndFeatureNamesAreUnique() throws Exception {
        String schema = Files.readString(Path.of("db/schema-reference.sql"));

        assertThat(schema)
                .contains("UNIQUE KEY uk_product_model (product_model)")
                .contains("UNIQUE KEY uk_feature_name (feature_name)");
    }

    @Test
    void opinionTableStoresFeatureCategoryTextWithoutFeatureForeignKey() throws Exception {
        String schema = Files.readString(Path.of("db/schema-reference.sql"));
        String opinionTable = schema.substring(
                schema.indexOf("CREATE TABLE IF NOT EXISTS pq_opinion"),
                schema.indexOf(") ENGINE=InnoDB", schema.indexOf("CREATE TABLE IF NOT EXISTS pq_opinion")));

        assertThat(opinionTable)
                .contains("feature_category_name VARCHAR(128) DEFAULT NULL")
                .doesNotContain("feature_id")
                .doesNotContain("fk_opinion_feature");
    }
}
