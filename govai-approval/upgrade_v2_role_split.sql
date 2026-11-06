-- ============================================================
-- GovAI 审批端 增量升级脚本 v2：审批流程多岗位分离
-- 库：govai_approval
-- 执行：mysql -uroot -p123456 --default-character-set=utf8mb4 govai_approval < upgrade_v2_role_split.sql
-- 幂等：可重复执行，不会重复加列 / 重复迁移
--
-- 岗位模型（受理与初审同岗，三岗位）：
--   受理(START)/初审(APPROVAL) -> APPROVER   (审批员/受理初审岗，张审批 approver1)
--   决定(APPROVAL 原 SUPERVISOR) -> DECIDE   (终审负责人)
--   办结(END)                    -> ISSUE    (办结/出证岗)
--
-- 变更：
--   1) approval_task    增加 process_role_code（任务环节岗位快照，鉴权/待办过滤用）
--                       增加 handle_detail    （办结岗办理时填写的证照/批文号、送达方式等 JSON）
--   2) approval_instance 增加 settle_mode     （MANUAL人工 / AUTO即办件·系统自动办结）
--   3) approval_process_node.assignee_role 存量迁移：决定 SUPERVISOR->DECIDE、办结 END->ISSUE
--   4) approval_task.process_role_code 按所属节点现值回填
-- 不 DROP 任何列/表
-- ============================================================

-- ------------------------------------------------------------
-- 1. approval_task 加列（8.x 不支持 ADD COLUMN IF NOT EXISTS，先查 information_schema 再 ALTER）
-- ------------------------------------------------------------
SET @has_prc = (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA='govai_approval' AND TABLE_NAME='approval_task' AND COLUMN_NAME='process_role_code');
SET @ddl_prc = IF(@has_prc=0,
    'ALTER TABLE approval_task ADD COLUMN process_role_code VARCHAR(32) NULL COMMENT ''任务环节岗位快照(RECEIVE/REVIEW/DECIDE/ISSUE/APPROVER)'' AFTER node_id',
    'SELECT 1');
PREPARE stmt_prc FROM @ddl_prc; EXECUTE stmt_prc; DEALLOCATE PREPARE stmt_prc;

SET @has_hd = (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA='govai_approval' AND TABLE_NAME='approval_task' AND COLUMN_NAME='handle_detail');
SET @ddl_hd = IF(@has_hd=0,
    'ALTER TABLE approval_task ADD COLUMN handle_detail TEXT NULL COMMENT ''办理附加信息(JSON):办结岗填证照/批文号、送达方式等'' AFTER approve_opinion',
    'SELECT 1');
PREPARE stmt_hd FROM @ddl_hd; EXECUTE stmt_hd; DEALLOCATE PREPARE stmt_hd;

-- 查询索引：按岗位+状态过滤待办
SET @has_idx = (SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA='govai_approval' AND TABLE_NAME='approval_task' AND INDEX_NAME='idx_role_status');
SET @ddl_idx = IF(@has_idx=0,
    'ALTER TABLE approval_task ADD KEY idx_role_status (process_role_code, task_status)',
    'SELECT 1');
PREPARE stmt_idx FROM @ddl_idx; EXECUTE stmt_idx; DEALLOCATE PREPARE stmt_idx;

-- ------------------------------------------------------------
-- 2. approval_instance 加 settle_mode
-- ------------------------------------------------------------
SET @has_sm = (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA='govai_approval' AND TABLE_NAME='approval_instance' AND COLUMN_NAME='settle_mode');
SET @ddl_sm = IF(@has_sm=0,
    'ALTER TABLE approval_instance ADD COLUMN settle_mode VARCHAR(16) NOT NULL DEFAULT ''MANUAL'' COMMENT ''MANUAL人工/AUTO即办件·系统自动办结'' AFTER status',
    'SELECT 1');
PREPARE stmt_sm FROM @ddl_sm; EXECUTE stmt_sm; DEALLOCATE PREPARE stmt_sm;

-- ------------------------------------------------------------
-- 3. approval_process_node.assignee_role 存量岗位迁移（幂等 UPDATE）
--    决定：SUPERVISOR -> DECIDE（终审负责人）
--    办结：END + APPROVER -> ISSUE（办结/出证岗）
--    受理/初审保留 APPROVER（受理初审同岗）
-- ------------------------------------------------------------
UPDATE approval_process_node
SET assignee_role = 'DECIDE'
WHERE assignee_role = 'SUPERVISOR' AND node_type = 'APPROVAL' AND node_name = '决定';

UPDATE approval_process_node
SET assignee_role = 'ISSUE'
WHERE assignee_role = 'APPROVER' AND node_type = 'END' AND node_name = '办结';

-- 兜底：未按中文名命名的同型节点（按 node_type 迁移，避免历史模板出现无人认领的旧角色）
UPDATE approval_process_node
SET assignee_role = 'DECIDE'
WHERE assignee_role = 'SUPERVISOR';

UPDATE approval_process_node
SET assignee_role = 'ISSUE'
WHERE assignee_role = 'APPROVER' AND node_type = 'END';

-- ------------------------------------------------------------
-- 4. approval_task.process_role_code 按所属节点现值回填（历史任务一并回填用于展示）
-- ------------------------------------------------------------
UPDATE approval_task t
JOIN approval_process_node n ON t.node_id = n.id
SET t.process_role_code = n.assignee_role
WHERE t.process_role_code IS NULL OR t.process_role_code = '';

-- 兜底：node_id 悬空或节点已删的任务，按实例当前节点所在环节推断不了则保留 NULL（理论不存在）
