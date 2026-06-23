-- MySQL 5.7 reference schema. If these tables already exist, only reconcile missing columns/indexes.

CREATE TABLE IF NOT EXISTS pq_product (
    id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    product_code    VARCHAR(64) NOT NULL,
    product_model   VARCHAR(128) NOT NULL,
    status          TINYINT UNSIGNED NOT NULL DEFAULT 1,
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_product_code (product_code),
    KEY idx_product_model (product_model)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS pq_feature (
    id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    feature_code    VARCHAR(64) NOT NULL,
    feature_name    VARCHAR(128) NOT NULL,
    sort_no         INT UNSIGNED NOT NULL DEFAULT 0,
    status          TINYINT UNSIGNED NOT NULL DEFAULT 1,
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_feature_code (feature_code),
    KEY idx_feature_status_sort (status, sort_no, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS pq_product_feature (
    product_id      BIGINT UNSIGNED NOT NULL,
    feature_id      BIGINT UNSIGNED NOT NULL,
    display_order   INT UNSIGNED NOT NULL DEFAULT 0,
    required_flag   TINYINT UNSIGNED NOT NULL DEFAULT 0,
    status          TINYINT UNSIGNED NOT NULL DEFAULT 1,
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (product_id, feature_id),
    KEY idx_pf_feature (feature_id, product_id),
    CONSTRAINT fk_pf_product FOREIGN KEY (product_id) REFERENCES pq_product(id),
    CONSTRAINT fk_pf_feature FOREIGN KEY (feature_id) REFERENCES pq_feature(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS pq_answer (
    id                      BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    source_system           VARCHAR(32) NOT NULL DEFAULT 'EXCEL',
    questionnaire_id        VARCHAR(128) NOT NULL,
    product_id              BIGINT UNSIGNED NOT NULL,
    answer_time             DATETIME NOT NULL,
    answer_date             DATE GENERATED ALWAYS AS (DATE(answer_time)) STORED,
    rom_version             VARCHAR(64) NOT NULL DEFAULT '',
    app_version             VARCHAR(64) NOT NULL DEFAULT '',
    feedback_text           TEXT,
    score_reason            TEXT,
    recommend_score         TINYINT UNSIGNED NOT NULL,
    user_category           TINYINT UNSIGNED NOT NULL DEFAULT 0,
    created_at              DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_source_questionnaire (source_system, questionnaire_id),
    KEY idx_answer_time (answer_time, id),
    KEY idx_answer_product_time (product_id, answer_time, id),
    KEY idx_answer_app_time (app_version, answer_time, id),
    KEY idx_answer_rom_time (rom_version, answer_time, id),
    CONSTRAINT fk_answer_product FOREIGN KEY (product_id) REFERENCES pq_product(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS pq_answer_feature_score (
    answer_id       BIGINT UNSIGNED NOT NULL,
    feature_id      BIGINT UNSIGNED NOT NULL,
    score           TINYINT UNSIGNED NOT NULL,
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (answer_id, feature_id),
    KEY idx_afs_feature_answer (feature_id, answer_id),
    CONSTRAINT fk_afs_answer FOREIGN KEY (answer_id) REFERENCES pq_answer(id) ON DELETE CASCADE,
    CONSTRAINT fk_afs_feature FOREIGN KEY (feature_id) REFERENCES pq_feature(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS pq_opinion (
    id                  BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    answer_id           BIGINT UNSIGNED NOT NULL,
    opinion_seq         SMALLINT UNSIGNED NOT NULL,
    sentiment_code      TINYINT UNSIGNED NOT NULL,
    feature_id          BIGINT UNSIGNED DEFAULT NULL,
    feedback_content_1  TEXT,
    feedback_content_2  TEXT,
    created_at          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_opinion_answer_seq (answer_id, opinion_seq),
    KEY idx_opinion_answer_sentiment_feature (answer_id, sentiment_code, feature_id),
    KEY idx_opinion_feature_sentiment_answer (feature_id, sentiment_code, answer_id),
    CONSTRAINT fk_opinion_answer FOREIGN KEY (answer_id) REFERENCES pq_answer(id) ON DELETE CASCADE,
    CONSTRAINT fk_opinion_feature FOREIGN KEY (feature_id) REFERENCES pq_feature(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
