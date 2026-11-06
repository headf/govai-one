-- ============================================================
-- GovAI 监管端 增量升级脚本 v2：审批岗位角色 + 演示账号
-- 库：govai_supervision（统一 SSO 主库 sys_user / sys_role）
-- 执行：mysql -uroot -p123456 --default-character-set=utf8mb4 govai_supervision < upgrade_v2_add_staff_roles.sql
-- 幂等：可重复执行，不会重复插入
--
-- 用途：审批端多岗位分离。审批端登录时按账号 role_code 识别岗位：
--   APPROVER -> 受理/初审岗（张审批 approver1，已在 init.sql）
--   DECIDE   -> 终审负责人（新增）
--   ISSUE    -> 办结/出证岗（新增）
-- DECIDE/ISSUE 仅用于登录审批端工作台，不授监管端后台权限码（非监管角色）。
-- 不 DROP 任何表/列。
-- ============================================================

-- ------------------------------------------------------------
-- 1. 新增角色（role_code 唯一，按 code NOT EXISTS 保证幂等；role_id 由 AUTO_INCREMENT 分配，不写死）
-- ------------------------------------------------------------
INSERT INTO sys_role (role_name, role_code, description)
SELECT '终审负责人', 'DECIDE', '审批端终审决定岗（领导签发）'
WHERE NOT EXISTS (SELECT 1 FROM sys_role WHERE role_code='DECIDE');

INSERT INTO sys_role (role_name, role_code, description)
SELECT '办结员', 'ISSUE', '审批端办结出证岗'
WHERE NOT EXISTS (SELECT 1 FROM sys_role WHERE role_code='ISSUE');

-- ------------------------------------------------------------
-- 2. 新增演示账号（密码 123456，BCrypt 串与 init.sql/upgrade.sql 完全一致）
--    role_id 按 role_code 反查，避免硬编码撞号
-- ------------------------------------------------------------
INSERT INTO sys_user (username, password, real_name, phone, department, role_id, status)
SELECT 'decide1', '$2a$10$gvai4/0LPs5a3eKHFhu0nOMZurDYVzGzhfVyTx6cwdVWqCSMIs0UG', '赵领导', '13800000011', '审批科', r.id, 1
FROM sys_role r WHERE r.role_code='DECIDE'
  AND NOT EXISTS (SELECT 1 FROM sys_user u WHERE u.username='decide1');

INSERT INTO sys_user (username, password, real_name, phone, department, role_id, status)
SELECT 'issue1', '$2a$10$gvai4/0LPs5a3eKHFhu0nOMZurDYVzGzhfVyTx6cwdVWqCSMIs0UG', '钱出证', '13800000012', '制证中心', r.id, 1
FROM sys_role r WHERE r.role_code='ISSUE'
  AND NOT EXISTS (SELECT 1 FROM sys_user u WHERE u.username='issue1');
