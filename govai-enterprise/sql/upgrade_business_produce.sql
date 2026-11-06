-- ============================================================
-- GovAI 企业端 增量升级：补全"办结产出"所需字段
-- 库：govai_enterprise
-- 幂等：可重复执行(8.4 不支持 ADD COLUMN IF NOT EXISTS,先查 information_schema)
-- 内容：
--   1) enterprise_info 加 business_scope 经营范围 / register_capital 注册资本(万) / business_term_until 经营期限至
--   2) 新建 subsidy_payment 补贴发放记录表(扶持补贴事项办结到账)
-- ============================================================

-- ------------------------------------------------------------
-- 1. enterprise_info 加列
-- ------------------------------------------------------------
SET @has_scope = (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA='govai_enterprise' AND TABLE_NAME='enterprise_info' AND COLUMN_NAME='business_scope');
SET @ddl_scope = IF(@has_scope=0,
    'ALTER TABLE enterprise_info ADD COLUMN business_scope VARCHAR(500) NULL COMMENT ''经营范围'' AFTER enterprise_type',
    'SELECT 1');
PREPARE stmt_scope FROM @ddl_scope; EXECUTE stmt_scope; DEALLOCATE PREPARE stmt_scope;

SET @has_capital = (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA='govai_enterprise' AND TABLE_NAME='enterprise_info' AND COLUMN_NAME='register_capital');
SET @ddl_capital = IF(@has_capital=0,
    'ALTER TABLE enterprise_info ADD COLUMN register_capital DECIMAL(14,2) NULL COMMENT ''注册资本(万元)'' AFTER business_scope',
    'SELECT 1');
PREPARE stmt_capital FROM @ddl_capital; EXECUTE stmt_capital; DEALLOCATE PREPARE stmt_capital;

SET @has_term = (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA='govai_enterprise' AND TABLE_NAME='enterprise_info' AND COLUMN_NAME='business_term_until');
SET @ddl_term = IF(@has_term=0,
    'ALTER TABLE enterprise_info ADD COLUMN business_term_until DATE NULL COMMENT ''经营期限至(长期为空)'' AFTER register_capital',
    'SELECT 1');
PREPARE stmt_term FROM @ddl_term; EXECUTE stmt_term; DEALLOCATE PREPARE stmt_term;

-- ------------------------------------------------------------
-- 2. 补贴发放记录表(扶持补贴/减免资格办结后到账)
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS subsidy_payment (
    id            BIGINT       PRIMARY KEY AUTO_INCREMENT,
    enterprise_id BIGINT       NOT NULL COMMENT '企业ID',
    matter_id     BIGINT       COMMENT '事项ID(补贴/减免事项)',
    record_id     BIGINT       COMMENT '申报记录ID',
    subsidy_type  VARCHAR(50)  COMMENT '补贴类型(稳岗返还/增值税减免等)',
    amount        DECIMAL(14,2) NOT NULL COMMENT '发放金额(元)',
    pay_time      DATETIME     COMMENT '到账时间',
    status        VARCHAR(20)  DEFAULT 'PAID' COMMENT 'PAID已发放',
    bank_name     VARCHAR(100) COMMENT '收款银行',
    bank_account  VARCHAR(50)  COMMENT '收款账号',
    create_time   DATETIME     DEFAULT CURRENT_TIMESTAMP,
    KEY idx_enterprise (enterprise_id),
    KEY idx_record (record_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='补贴发放记录(扶持补贴办结到账)';
