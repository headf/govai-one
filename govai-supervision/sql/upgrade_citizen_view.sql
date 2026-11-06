-- ============================================================
-- GovAI 监管端: approval_data_cache 增加公民身份列(幂等)
-- 库: govai_supervision
-- 执行: mysql -uroot -p123456 --default-character-set=utf8mb4 govai_supervision < upgrade_citizen_view.sql
-- 目的: 群众端办件上报补全 PERSONAL 办件的公民关联, 支撑"公民个人画像"(按 sys_user.id 聚合办件)。
-- 8.4 不支持 ADD COLUMN IF NOT EXISTS, 先查 information_schema 再 ALTER。
-- ============================================================
USE govai_supervision;

SET @has_uid = (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = 'govai_supervision' AND TABLE_NAME = 'approval_data_cache' AND COLUMN_NAME = 'applicant_user_id');
SET @sql1 = IF(@has_uid = 0,
    'ALTER TABLE approval_data_cache ADD COLUMN applicant_user_id BIGINT NULL COMMENT ''申报公民ID(监管端sys_user.id)'' AFTER enterprise_name',
    'SELECT 1');
PREPARE s1 FROM @sql1; EXECUTE s1; DEALLOCATE PREPARE s1;

SET @has_phone = (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = 'govai_supervision' AND TABLE_NAME = 'approval_data_cache' AND COLUMN_NAME = 'applicant_phone');
SET @sql2 = IF(@has_phone = 0,
    'ALTER TABLE approval_data_cache ADD COLUMN applicant_phone VARCHAR(20) NULL COMMENT ''申报人手机号'' AFTER applicant_user_id',
    'SELECT 1');
PREPARE s2 FROM @sql2; EXECUTE s2; DEALLOCATE PREPARE s2;

SET @has_idcard = (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = 'govai_supervision' AND TABLE_NAME = 'approval_data_cache' AND COLUMN_NAME = 'applicant_id_card');
SET @sql3 = IF(@has_idcard = 0,
    'ALTER TABLE approval_data_cache ADD COLUMN applicant_id_card VARCHAR(32) NULL COMMENT ''申报人证件号(身份证)'' AFTER applicant_phone',
    'SELECT 1');
PREPARE s3 FROM @sql3; EXECUTE s3; DEALLOCATE PREPARE s3;
