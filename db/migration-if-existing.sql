-- Run only the statements that are missing in your existing schema.
-- status 控制产品是否进入模板“产品字典”和新导入校验；0 只表示停用/软删除，不清理历史外键。
ALTER TABLE pq_product
    ADD COLUMN status TINYINT UNSIGNED NOT NULL DEFAULT 1 AFTER product_model;

-- 支持模板下载和导入校验稳定读取启用产品。
CREATE INDEX idx_product_status
    ON pq_product (status, id);

-- sort_no 控制新下载模板中启用特性动态列的稳定顺序。
ALTER TABLE pq_feature
    ADD COLUMN sort_no INT UNSIGNED NOT NULL DEFAULT 0 AFTER feature_name;

-- 支持模板下载和导入表头校验按 sort_no、id 稳定读取启用特性。
CREATE INDEX idx_feature_status_sort
    ON pq_feature (status, sort_no, id);

-- 支持按 source_system + questionnaire_id 幂等覆盖导入；已有唯一索引时跳过。
CREATE UNIQUE INDEX uk_source_questionnaire
    ON pq_answer (source_system, questionnaire_id);
