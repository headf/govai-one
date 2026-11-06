-- 申报记录表加审批退回/补正意见字段(用于企业端展示审批端的 REPAIR/REJECTED 原因)
ALTER TABLE ent_application_record ADD COLUMN remark VARCHAR(500) NULL COMMENT '审批端退回/补正意见(REPAIR/REJECTED 时写入)';

-- 验证
SELECT id, record_code, status, remark FROM ent_application_record;
