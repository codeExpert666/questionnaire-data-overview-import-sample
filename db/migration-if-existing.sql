-- Run only the statements that are missing in your existing schema.
ALTER TABLE pq_feature
    ADD COLUMN sort_no INT UNSIGNED NOT NULL DEFAULT 0 AFTER feature_name;

CREATE INDEX idx_feature_status_sort
    ON pq_feature (status, sort_no, id);

-- Required for idempotent upsert. Skip if it already exists.
CREATE UNIQUE INDEX uk_source_questionnaire
    ON pq_answer (source_system, questionnaire_id);
