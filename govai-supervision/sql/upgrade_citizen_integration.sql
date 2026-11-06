-- ============================================================
-- GovAI 监管端: 群众端接入 SSO 的公民账号 + 个人信用种子(幂等)
-- 库: govai_supervision
-- 执行: mysql -uroot -p123456 --default-character-set=utf8mb4 govai_supervision < upgrade_citizen_integration.sql
-- 幂等: username / credit_personal.user_id NOT EXISTS 即跳过,可重复执行。
-- 说明: 密码为全组一致的 BCrypt('123456')(与 admin/approver1 同一 hash 串)。
-- 演示公民账号:
--   citizen01 / 13800000020 / 王小明 / 信用 88 A
--   citizen02 / 13800000021 / 李华 / 信用 76 B
-- ============================================================

USE govai_supervision;

-- 1) 公民演示账号(role_code=CITIZEN)
INSERT INTO sys_user (username, password, real_name, phone, role_id, status, create_time)
SELECT 'citizen01', '$2a$10$gvai4/0LPs5a3eKHFhu0nOMZurDYVzGzhfVyTx6cwdVWqCSMIs0UG', '王小明', '13800000020',
       (SELECT id FROM sys_role WHERE role_code = 'CITIZEN'), 1, NOW()
WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE username = 'citizen01');

INSERT INTO sys_user (username, password, real_name, phone, role_id, status, create_time)
SELECT 'citizen02', '$2a$10$gvai4/0LPs5a3eKHFhu0nOMZurDYVzGzhfVyTx6cwdVWqCSMIs0UG', '李华', '13800000021',
       (SELECT id FROM sys_role WHERE role_code = 'CITIZEN'), 1, NOW()
WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE username = 'citizen02');

-- 2) 公民个人信用 seed(credit_no 演示固定编号,user_id=sys_user.id)
INSERT INTO credit_personal (credit_no, user_id, credit_score, credit_level, update_time)
SELECT 'CX2026090900101', s.id, 88.00, 'A', NOW()
FROM sys_user s WHERE s.username = 'citizen01'
  AND NOT EXISTS (SELECT 1 FROM credit_personal c WHERE c.user_id = s.id);

INSERT INTO credit_personal (credit_no, user_id, credit_score, credit_level, update_time)
SELECT 'CX2026090900102', s.id, 76.00, 'B', NOW()
FROM sys_user s WHERE s.username = 'citizen02'
  AND NOT EXISTS (SELECT 1 FROM credit_personal c WHERE c.user_id = s.id);
