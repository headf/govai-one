-- ============================================================
-- GovAI 监管端 增量升级脚本（在 init.sql 之后执行）
-- 幂等：可重复执行，不会重复插入/重复授权
-- 内容：
--   1) risk_alert 增加处置意见/处置人列（8.4 不支持 ADD COLUMN IF NOT EXISTS，先查 information_schema）
--   2) 补齐 sys_permission 按钮/API 级权限码
--   3) 给 ADMIN(role_id=1) 全量授权、SUPERVISOR(role_id=3) 授业务权限
-- 执行：mysql -uroot -p123456 --default-character-set=utf8mb4 govai_supervision < upgrade.sql
-- ============================================================

-- ------------------------------------------------------------
-- 1. risk_alert 增加处置留痕列（不存在才加）
-- ------------------------------------------------------------
SET @has_opinion = (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA='govai_supervision' AND TABLE_NAME='risk_alert' AND COLUMN_NAME='handle_opinion');
SET @ddl_opinion = IF(@has_opinion=0,
    'ALTER TABLE risk_alert ADD COLUMN handle_opinion VARCHAR(500) NULL COMMENT ''处置意见'' AFTER handle_status',
    'SELECT 1');
PREPARE stmt_opinion FROM @ddl_opinion; EXECUTE stmt_opinion; DEALLOCATE PREPARE stmt_opinion;

SET @has_handler = (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA='govai_supervision' AND TABLE_NAME='risk_alert' AND COLUMN_NAME='handler_id');
SET @ddl_handler = IF(@has_handler=0,
    'ALTER TABLE risk_alert ADD COLUMN handler_id BIGINT NULL COMMENT ''处置人ID'' AFTER handle_opinion',
    'SELECT 1');
PREPARE stmt_handler FROM @ddl_handler; EXECUTE stmt_handler; DEALLOCATE PREPARE stmt_handler;

-- ------------------------------------------------------------
-- 2. 补齐权限码（BUTTON/API 型，parent_id 挂到对应菜单权限下）
--    插入用 NOT EXISTS(permission_code) 保证幂等
-- ------------------------------------------------------------
-- 用户管理（父：user:list）
INSERT INTO sys_permission (permission_name, permission_code, type, parent_id, sort_order)
SELECT '用户新增', 'user:add', 'BUTTON', p.id, 1 FROM sys_permission p WHERE p.permission_code='user:list'
    AND NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code='user:add');
INSERT INTO sys_permission (permission_name, permission_code, type, parent_id, sort_order)
SELECT '用户编辑', 'user:edit', 'BUTTON', p.id, 2 FROM sys_permission p WHERE p.permission_code='user:list'
    AND NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code='user:edit');
INSERT INTO sys_permission (permission_name, permission_code, type, parent_id, sort_order)
SELECT '用户删除', 'user:delete', 'BUTTON', p.id, 3 FROM sys_permission p WHERE p.permission_code='user:list'
    AND NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code='user:delete');
INSERT INTO sys_permission (permission_name, permission_code, type, parent_id, sort_order)
SELECT '重置密码', 'user:reset-pwd', 'BUTTON', p.id, 4 FROM sys_permission p WHERE p.permission_code='user:list'
    AND NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code='user:reset-pwd');

-- 角色管理（父：role:list）
INSERT INTO sys_permission (permission_name, permission_code, type, parent_id, sort_order)
SELECT '角色新增', 'role:add', 'BUTTON', p.id, 1 FROM sys_permission p WHERE p.permission_code='role:list'
    AND NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code='role:add');
INSERT INTO sys_permission (permission_name, permission_code, type, parent_id, sort_order)
SELECT '角色编辑', 'role:edit', 'BUTTON', p.id, 2 FROM sys_permission p WHERE p.permission_code='role:list'
    AND NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code='role:edit');
INSERT INTO sys_permission (permission_name, permission_code, type, parent_id, sort_order)
SELECT '角色删除', 'role:delete', 'BUTTON', p.id, 3 FROM sys_permission p WHERE p.permission_code='role:list'
    AND NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code='role:delete');
INSERT INTO sys_permission (permission_name, permission_code, type, parent_id, sort_order)
SELECT '角色授权', 'role:grant', 'BUTTON', p.id, 4 FROM sys_permission p WHERE p.permission_code='role:list'
    AND NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code='role:grant');

-- 权限管理（父：user:list 之下独立菜单——沿用现有权限树挂在 user:list 兄弟？骨架里权限管理就是 user:list 级菜单，挂独立）
INSERT INTO sys_permission (permission_name, permission_code, type, parent_id, sort_order)
SELECT '权限管理菜单', 'perm:list', 'MENU', 0, 90 FROM DUAL
    WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code='perm:list');
INSERT INTO sys_permission (permission_name, permission_code, type, parent_id, sort_order)
SELECT '权限新增', 'perm:add', 'BUTTON', p.id, 1 FROM sys_permission p WHERE p.permission_code='perm:list'
    AND NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code='perm:add');
INSERT INTO sys_permission (permission_name, permission_code, type, parent_id, sort_order)
SELECT '权限编辑', 'perm:edit', 'BUTTON', p.id, 2 FROM sys_permission p WHERE p.permission_code='perm:list'
    AND NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code='perm:edit');
INSERT INTO sys_permission (permission_name, permission_code, type, parent_id, sort_order)
SELECT '权限删除', 'perm:delete', 'BUTTON', p.id, 3 FROM sys_permission p WHERE p.permission_code='perm:list'
    AND NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code='perm:delete');

-- 信用管理（父：credit:list）
INSERT INTO sys_permission (permission_name, permission_code, type, parent_id, sort_order)
SELECT '企业信用评分', 'credit:score', 'BUTTON', p.id, 1 FROM sys_permission p WHERE p.permission_code='credit:list'
    AND NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code='credit:score');
INSERT INTO sys_permission (permission_name, permission_code, type, parent_id, sort_order)
SELECT '信用修复审核', 'repair:audit', 'BUTTON', p.id, 2 FROM sys_permission p WHERE p.permission_code='credit:list'
    AND NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code='repair:audit');

-- 风险预警（父：risk:list）
INSERT INTO sys_permission (permission_name, permission_code, type, parent_id, sort_order)
SELECT '预警规则管理', 'risk:manage', 'BUTTON', p.id, 1 FROM sys_permission p WHERE p.permission_code='risk:list'
    AND NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code='risk:manage');
INSERT INTO sys_permission (permission_name, permission_code, type, parent_id, sort_order)
SELECT '预警处置', 'alert:handle', 'BUTTON', p.id, 2 FROM sys_permission p WHERE p.permission_code='risk:list'
    AND NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code='alert:handle');

-- 抽样检查（父：sample:list）
INSERT INTO sys_permission (permission_name, permission_code, type, parent_id, sort_order)
SELECT '抽样检查操作', 'sample:operate', 'BUTTON', p.id, 1 FROM sys_permission p WHERE p.permission_code='sample:list'
    AND NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code='sample:operate');

-- 审计追溯（父：audit:list）
INSERT INTO sys_permission (permission_name, permission_code, type, parent_id, sort_order)
SELECT '审计补录', 'audit:record', 'BUTTON', p.id, 1 FROM sys_permission p WHERE p.permission_code='audit:list'
    AND NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code='audit:record');

-- 字典管理（父：stats:view 同级新增菜单，挂在统计旁）
INSERT INTO sys_permission (permission_name, permission_code, type, parent_id, sort_order)
SELECT '数据字典菜单', 'dict:list', 'MENU', 0, 91 FROM DUAL
    WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code='dict:list');
INSERT INTO sys_permission (permission_name, permission_code, type, parent_id, sort_order)
SELECT '字典管理', 'dict:manage', 'BUTTON', p.id, 1 FROM sys_permission p WHERE p.permission_code='dict:list'
    AND NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code='dict:manage');

-- 操作日志
INSERT INTO sys_permission (permission_name, permission_code, type, parent_id, sort_order)
SELECT '操作日志菜单', 'log:list', 'MENU', 0, 92 FROM DUAL
    WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code='log:list');
INSERT INTO sys_permission (permission_name, permission_code, type, parent_id, sort_order)
SELECT '日志清空', 'log:clear', 'BUTTON', p.id, 1 FROM sys_permission p WHERE p.permission_code='log:list'
    AND NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code='log:clear');

-- 审批数据（真实流程实时办件，只读页）
INSERT INTO sys_permission (permission_name, permission_code, type, parent_id, sort_order)
SELECT '审批数据菜单', 'approval:list', 'MENU', 0, 93 FROM DUAL
    WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code='approval:list');

-- 去模拟化：移除历史遗留的"模拟推送/补录"与"审批缓存删除"按钮权限及其角色授权（幂等）
DELETE FROM sys_role_permission WHERE permission_id IN
    (SELECT id FROM sys_permission WHERE permission_code IN ('approval:push', 'approval:delete'));
DELETE FROM sys_permission WHERE permission_code IN ('approval:push', 'approval:delete');

-- ------------------------------------------------------------
-- 3. 授权：ADMIN(role_id=1) 全量；SUPERVISOR(role_id=3) 业务权限
--    以 permission_code 关联，天然幂等（role-permission 主键去重）
-- ------------------------------------------------------------
-- ADMIN：把全部权限码授给角色 1
INSERT INTO sys_role_permission (role_id, permission_id)
SELECT 1, id FROM sys_permission
WHERE id NOT IN (SELECT permission_id FROM sys_role_permission WHERE role_id=1);

-- SUPERVISOR：授业务类权限码（查/业务写，不含系统管理写、不含角色授权、日志清空）
INSERT INTO sys_role_permission (role_id, permission_id)
SELECT 3, id FROM sys_permission
WHERE permission_code IN (
    'dashboard', 'stats:view', 'user:list',
    'credit:list', 'credit:score', 'repair:audit',
    'risk:list', 'risk:manage', 'alert:handle',
    'sample:list', 'sample:operate',
    'audit:list', 'audit:record',
    'approval:list'
)
AND id NOT IN (SELECT permission_id FROM sys_role_permission WHERE role_id=3);

-- SUPERVISOR 同时保留大屏；admin 那条已含全部
-- APPROVER/其他角色本轮不预置（默认无后台权限，只可登录看大屏，后续在角色授权页自行配置）

-- ------------------------------------------------------------
-- 4. 风险规则语义修正（幂等 UPDATE）
--    TIMEOUT 需阈值小时数（48h）；将"满意度低于阈值"(不可实现) 改为 ANOMALY 重复申报演示
-- ------------------------------------------------------------
UPDATE risk_rule SET threshold_value='48'
    WHERE rule_type='TIMEOUT' AND (threshold_value IS NULL OR threshold_value='');
UPDATE risk_rule SET rule_name='重复申报异常', rule_type='ANOMALY', threshold_value='5'
    WHERE rule_name='满意度低于阈值';

-- ------------------------------------------------------------
-- 5. 审批数据缓存加企业维度列(企业端真实申报上报)
--    8.4 不支持 ADD COLUMN IF NOT EXISTS,先查 information_schema 再 ALTER
-- ------------------------------------------------------------
SET @has_ent_id = (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA='govai_supervision' AND TABLE_NAME='approval_data_cache' AND COLUMN_NAME='enterprise_id');
SET @ddl_ent_id = IF(@has_ent_id=0,
    'ALTER TABLE approval_data_cache ADD COLUMN enterprise_id BIGINT NULL COMMENT ''申报企业ID(enterprise_info.id)'' AFTER process_duration_hours',
    'SELECT 1');
PREPARE stmt_ent_id FROM @ddl_ent_id; EXECUTE stmt_ent_id; DEALLOCATE PREPARE stmt_ent_id;

SET @has_ent_name = (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA='govai_supervision' AND TABLE_NAME='approval_data_cache' AND COLUMN_NAME='enterprise_name');
SET @ddl_ent_name = IF(@has_ent_name=0,
    'ALTER TABLE approval_data_cache ADD COLUMN enterprise_name VARCHAR(128) NULL COMMENT ''申报企业名称'' AFTER enterprise_id',
    'SELECT 1');
PREPARE stmt_ent_name FROM @ddl_ent_name; EXECUTE stmt_ent_name; DEALLOCATE PREPARE stmt_ent_name;

-- ------------------------------------------------------------
-- 6. 两端互通演示种子(监管端给企业端 enterprise_info.id=1/2 建档)
--    幂等:仅当对应 enterprise_id 尚不存在信用档案时插入
-- ------------------------------------------------------------
INSERT INTO credit_enterprise (enterprise_id, credit_score, credit_level, dimension_scores)
SELECT 1, 88.0, 'A', '{"合规经营":30,"履约情况":28,"社会责任":18}'
WHERE NOT EXISTS (SELECT 1 FROM credit_enterprise WHERE enterprise_id=1);
INSERT INTO credit_enterprise (enterprise_id, credit_score, credit_level, dimension_scores)
SELECT 2, 62.0, 'C', '{"合规经营":22,"履约情况":18,"社会责任":14}'
WHERE NOT EXISTS (SELECT 1 FROM credit_enterprise WHERE enterprise_id=2);

INSERT INTO risk_alert (rule_id, target_type, target_id, alert_level, alert_content, alert_time, handle_status)
SELECT rp.id, 'ENTERPRISE', 1, 'MEDIUM', '企业【东软智膳餐饮管理有限公司】近30日申办事项超阈,请加强关注', NOW(), 'PENDING'
FROM risk_rule rp WHERE NOT EXISTS (SELECT 1 FROM risk_alert WHERE target_type='ENTERPRISE' AND target_id=1) LIMIT 1;

INSERT INTO risk_alert (rule_id, target_type, target_id, alert_level, alert_content, alert_time, handle_status)
SELECT rp.id, 'ENTERPRISE', 2, 'HIGH', '企业【云启科技有限公司】统一信用代码主体存在经营异常风险,建议专项检查', NOW(), 'PENDING'
FROM risk_rule rp WHERE NOT EXISTS (SELECT 1 FROM risk_alert WHERE target_type='ENTERPRISE' AND target_id=2) LIMIT 1;

INSERT INTO credit_repair (target_type, target_id, repair_reason, apply_time, audit_status)
SELECT 'ENTERPRISE', 1, '已按要求完成整改,申请修复信用', NOW(), 'PENDING'
WHERE NOT EXISTS (SELECT 1 FROM credit_repair WHERE target_type='ENTERPRISE' AND target_id=1);

INSERT INTO credit_repair (target_type, target_id, repair_reason, apply_time, audit_status, audit_opinion, audit_time)
SELECT 'ENTERPRISE', 2, '证照过期已及时延续办理', DATE_SUB(NOW(), INTERVAL 3 DAY), 'PASS', '整改属实,准予修复', DATE_SUB(NOW(), INTERVAL 2 DAY)
WHERE NOT EXISTS (SELECT 1 FROM credit_repair WHERE target_type='ENTERPRISE' AND target_id=2);

-- ------------------------------------------------------------
-- 7. 统一 SSO: sys_user 加企业归属列 + 存量企业账号迁移
--    企业端登录改为统一走监管端 /auth/login(契约#1),凭证主库=sys_user。
--    企业账号 role_id=5(ENTERPRISE),enterprise_id=其所属企业(enterprise_info.id)。
--    幂等:加列先查 information_schema;迁移按 username NOT EXISTS。
-- ------------------------------------------------------------
SET @has_sys_ent = (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA='govai_supervision' AND TABLE_NAME='sys_user' AND COLUMN_NAME='enterprise_id');
SET @ddl_sys_ent = IF(@has_sys_ent=0,
    'ALTER TABLE sys_user ADD COLUMN enterprise_id BIGINT NULL COMMENT ''企业账号所属企业(enterprise_info.id),非企业账号为空'' AFTER role_id',
    'SELECT 1');
PREPARE stmt_sys_ent FROM @ddl_sys_ent; EXECUTE stmt_sys_ent; DEALLOCATE PREPARE stmt_sys_ent;

-- 存量企业账号迁移(同一 MySQL 实例跨库;密码 BCrypt 直拷,监管端 AuthService 用同一 BCrypt 匹配)
INSERT INTO sys_user (username, password, real_name, phone, role_id, enterprise_id, status, create_time)
SELECT e.username, e.password, e.real_name, e.phone, 5, e.enterprise_id, e.status, e.create_time
FROM govai_enterprise.enterprise_user e
WHERE NOT EXISTS (SELECT 1 FROM sys_user s WHERE s.username = e.username);

-- ------------------------------------------------------------
-- 8. 企业信用接入办事流(审批端按信用代码关联 + 差异化抽查演示种子)
--    8.1 credit_enterprise 落统一社会信用代码 credit_code(真列),跨库从企业端回填;
--    8.2 演示:企业2(云启科技)信用 62/C -> 55/D,用于演示"D级申办补贴类被拦截";
--         修复 PASS +5 -> 60/C 恰好放行,语义自洽。
--    幂等:加列先查 information_schema;回填仅补空;调级仅当当前非 D。
-- ------------------------------------------------------------
SET @has_ce_cc = (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA='govai_supervision' AND TABLE_NAME='credit_enterprise' AND COLUMN_NAME='credit_code');
SET @ddl_ce_cc = IF(@has_ce_cc=0,
    'ALTER TABLE credit_enterprise ADD COLUMN credit_code VARCHAR(32) NULL COMMENT ''统一社会信用代码(审批端按此关联企业信用)'' AFTER enterprise_id',
    'SELECT 1');
PREPARE stmt_ce_cc FROM @ddl_ce_cc; EXECUTE stmt_ce_cc; DEALLOCATE PREPARE stmt_ce_cc;

UPDATE credit_enterprise ce
JOIN govai_enterprise.enterprise_info ei ON ce.enterprise_id = ei.id
SET ce.credit_code = ei.credit_code
WHERE ce.credit_code IS NULL OR ce.credit_code = '';

UPDATE credit_enterprise
SET credit_score = 55.0, credit_level = 'D'
WHERE enterprise_id = 2 AND credit_level <> 'D';

-- ------------------------------------------------------------
-- 9. 记录型业务落"真业务单号"(试点:抽查/预警/修复/个人信用),前端展示替代裸自增id
--    幂等:先查 information_schema 再 ALTER;存量回填仅当单号为空。
-- ------------------------------------------------------------
SET @has_sample_no = (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA='govai_supervision' AND TABLE_NAME='supervision_sample' AND COLUMN_NAME='sample_no');
SET @ddl_sample_no = IF(@has_sample_no=0,
    'ALTER TABLE supervision_sample ADD COLUMN sample_no VARCHAR(32) NULL COMMENT ''抽查单号(CJ+日期+随机)'' AFTER id',
    'SELECT 1');
PREPARE stmt_sample_no FROM @ddl_sample_no; EXECUTE stmt_sample_no; DEALLOCATE PREPARE stmt_sample_no;
UPDATE supervision_sample SET sample_no = CONCAT('CJ', DATE_FORMAT(check_time, '%Y%m%d'), LPAD(id, 6, '0'))
WHERE sample_no IS NULL OR sample_no = '';

SET @has_alert_no = (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA='govai_supervision' AND TABLE_NAME='risk_alert' AND COLUMN_NAME='alert_no');
SET @ddl_alert_no = IF(@has_alert_no=0,
    'ALTER TABLE risk_alert ADD COLUMN alert_no VARCHAR(32) NULL COMMENT ''预警编号(YJ+日期+随机)'' AFTER id',
    'SELECT 1');
PREPARE stmt_alert_no FROM @ddl_alert_no; EXECUTE stmt_alert_no; DEALLOCATE PREPARE stmt_alert_no;
UPDATE risk_alert SET alert_no = CONCAT('YJ', DATE_FORMAT(alert_time, '%Y%m%d'), LPAD(id, 6, '0'))
WHERE alert_no IS NULL OR alert_no = '';

SET @has_repair_no = (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA='govai_supervision' AND TABLE_NAME='credit_repair' AND COLUMN_NAME='repair_no');
SET @ddl_repair_no = IF(@has_repair_no=0,
    'ALTER TABLE credit_repair ADD COLUMN repair_no VARCHAR(32) NULL COMMENT ''修复单号(XF+日期+随机)'' AFTER id',
    'SELECT 1');
PREPARE stmt_repair_no FROM @ddl_repair_no; EXECUTE stmt_repair_no; DEALLOCATE PREPARE stmt_repair_no;
UPDATE credit_repair SET repair_no = CONCAT('XF', DATE_FORMAT(apply_time, '%Y%m%d'), LPAD(id, 6, '0'))
WHERE repair_no IS NULL OR repair_no = '';

SET @has_credit_no = (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA='govai_supervision' AND TABLE_NAME='credit_personal' AND COLUMN_NAME='credit_no');
SET @ddl_credit_no = IF(@has_credit_no=0,
    'ALTER TABLE credit_personal ADD COLUMN credit_no VARCHAR(32) NULL COMMENT ''个人信用单号(CX+日期+随机)'' AFTER id',
    'SELECT 1');
PREPARE stmt_credit_no FROM @ddl_credit_no; EXECUTE stmt_credit_no; DEALLOCATE PREPARE stmt_credit_no;
UPDATE credit_personal SET credit_no = CONCAT('CX', DATE_FORMAT(update_time, '%Y%m%d'), LPAD(id, 6, '0'))
WHERE credit_no IS NULL OR credit_no = '';
