-- 证照分类:企业证照 vs 个人证照
ALTER TABLE enterprise_license ADD COLUMN category VARCHAR(20) DEFAULT 'ENTERPRISE' COMMENT '证照类别 ENTERPRISE企业/PERSONAL个人';

-- 个人证照(法人/经办人身份证等)
UPDATE enterprise_license SET category = 'PERSONAL'
WHERE license_type LIKE '%身份证%' OR license_type LIKE '%户口%' OR license_type LIKE '%社保%';

-- 企业证照(营业执照/许可证/资质证书等,默认 ENTERPRISE)
UPDATE enterprise_license SET category = 'ENTERPRISE'
WHERE license_type LIKE '%营业执照%' OR license_type LIKE '%许可%' OR license_type LIKE '%资质%' OR license_type LIKE '%证书%';

-- 验证
SELECT id, license_type, category FROM enterprise_license ORDER BY category, id;
