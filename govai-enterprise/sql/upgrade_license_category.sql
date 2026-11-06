-- 给 enterprise_license 表补充 category 字段,用于区分企业证照/个人证照
ALTER TABLE enterprise_license ADD COLUMN IF NOT EXISTS category VARCHAR(20) DEFAULT 'ENTERPRISE' COMMENT 'ENTERPRISE-企业证照 PERSONAL-个人证照';

-- 法人身份证归类为 PERSONAL
UPDATE enterprise_license SET category = 'PERSONAL' WHERE license_type LIKE '%身份证%';

-- 其余默认 ENTERPRISE
UPDATE enterprise_license SET category = 'ENTERPRISE' WHERE category IS NULL;

SELECT id, license_type, category FROM enterprise_license ORDER BY id;
