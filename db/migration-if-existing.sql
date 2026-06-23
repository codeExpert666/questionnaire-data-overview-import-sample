-- Run only the statements that are missing in your existing schema.
-- status controls whether a product enters the template product dictionary and new import validation.
ALTER TABLE pq_product
    ADD COLUMN status TINYINT UNSIGNED NOT NULL DEFAULT 1 AFTER product_model;

-- Supports enabled product lookup for template download and import validation.
CREATE INDEX idx_product_status
    ON pq_product (status, id);

-- sort_no controls the stable order of enabled pq_feature columns in newly downloaded templates.
ALTER TABLE pq_feature
    ADD COLUMN sort_no INT UNSIGNED NOT NULL DEFAULT 0 AFTER feature_name;

-- Supports the template/import query: enabled features ordered by sort_no and id.
CREATE INDEX idx_feature_status_sort
    ON pq_feature (status, sort_no, id);

-- Required for idempotent upsert. Skip if it already exists.
CREATE UNIQUE INDEX uk_source_questionnaire
    ON pq_answer (source_system, questionnaire_id);
