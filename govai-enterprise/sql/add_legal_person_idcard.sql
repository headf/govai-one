-- 企业表加法人身份证号字段 + 填数据(用于法人管理页面)
ALTER TABLE enterprise_info ADD COLUMN legal_person_id_card VARCHAR(18) COMMENT '法定代表人身份证号';

-- 填入法人身份证号(与证照库一致)
UPDATE enterprise_info SET legal_person_id_card = '110108199001011234' WHERE id = 1;
UPDATE enterprise_info SET legal_person_id_card = '110108199202022345' WHERE id = 2;

-- 验证
SELECT id, enterprise_name, legal_person, legal_person_id_card FROM enterprise_info;
