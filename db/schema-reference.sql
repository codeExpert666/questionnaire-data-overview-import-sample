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
    -- 对外稳定编码：出现在 Excel 动态表头和“特性分类编码”列，创建后不应修改。
    feature_code    VARCHAR(64) NOT NULL,
    -- 展示名称：出现在模板表头和特性字典页，名称变化会要求用户重新下载模板。
    feature_name    VARCHAR(128) NOT NULL,
    -- 全量启用特性在模板中的排序号；相同排序号按 id 稳定排序。
    sort_no         INT UNSIGNED NOT NULL DEFAULT 0,
    -- 1=启用，进入新模板和导入校验；0=停用/软删除，历史外键仍保留。
    status          TINYINT UNSIGNED NOT NULL DEFAULT 1,
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_feature_code (feature_code),
    KEY idx_feature_status_sort (status, sort_no, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS pq_product_feature (
    product_id      BIGINT UNSIGNED NOT NULL,
    -- 适用关系只描述产品是否支持该特性，不决定模板是否生成该列。
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
    -- 来自 pq_feature.id；导入时只写入非空且产品适用的特性评分。
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
    -- 观点归属特性，可为空；填写时必须是当前启用且产品适用的特性。
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
