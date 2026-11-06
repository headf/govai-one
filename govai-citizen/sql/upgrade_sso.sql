-- ============================================================
-- GovAI 群众端: 接入监管端 SSO 的本地表扩展(幂等)
-- 库: govai_citizen
-- 执行: mysql -uroot -p123456 --default-character-set=utf8mb4 govai_citizen < upgrade_sso.sql
-- 目的: citizen_user 增加 sys_user_id 关联列(指向监管端 govai_supervision.sys_user.id)，
--       群众端 JwtInterceptor 据此把透传 token 的 sys_user.id 映射回本地 citizen_user.id。
-- 8.4 不支持 ADD COLUMN IF NOT EXISTS，先查 information_schema 再 ALTER。
-- ============================================================
USE govai_citizen;

SET @has_sys_user_id = (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = 'govai_citizen' AND TABLE_NAME = 'citizen_user' AND COLUMN_NAME = 'sys_user_id');
SET @ddl_sql = IF(@has_sys_user_id = 0,
    'ALTER TABLE citizen_user ADD COLUMN sys_user_id BIGINT NULL COMMENT ''监管端sys_user.id(SSO映射)'' AFTER phone',
    'SELECT 1');
PREPARE stmt FROM @ddl_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- sys_user_id 唯一索引(允许 NULL 多行)
SET @has_uk = (SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = 'govai_citizen' AND TABLE_NAME = 'citizen_user' AND INDEX_NAME = 'uk_sys_user_id');
SET @ddl_uk = IF(@has_uk = 0,
    'ALTER TABLE citizen_user ADD UNIQUE KEY uk_sys_user_id (sys_user_id)',
    'SELECT 1');
PREPARE stmt2 FROM @ddl_uk;
EXECUTE stmt2;
DEALLOCATE PREPARE stmt2;
