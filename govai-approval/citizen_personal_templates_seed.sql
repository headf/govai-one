-- GovAI 审批端: 群众端个人办事事项补流程模板(幂等)
-- 目的: 群众端(govai-citizen) 事项 matterCode 作 templateCode 提交审批端时,
--       审批端须有对应启用模板才能真实流转。此处为个人事项 GA-HK-001/SB-JY-001/YL-BX-001
--       各补一套 受理→初审→决定→办结 通用模板(岗位码对齐活库: APPROVER/APPROVER/DECIDE/ISSUE)。
-- 仅当编码不存在时插入,不删不改已有模板/实例。无规则集 => 引擎 TO_MANUAL 转人工首环节。
-- 执行: mysql -uroot -p123456 --default-character-set=utf8mb4 govai_approval < citizen_personal_templates_seed.sql

USE govai_approval;

DROP PROCEDURE IF EXISTS add_citizen_template_if_missing;
DELIMITER $$
CREATE PROCEDURE add_citizen_template_if_missing(
    IN t_code VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci,
    IN t_name VARCHAR(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci,
    IN m_type VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci)
BEGIN
    DECLARE tid BIGINT;
    DECLARE n1,n2,n3,n4 BIGINT;
    IF NOT EXISTS (SELECT 1 FROM approval_process_template WHERE template_code = t_code COLLATE utf8mb4_general_ci) THEN
        INSERT INTO approval_process_template (template_code, template_name, version, matter_type, status, description)
        VALUES (t_code, t_name, '1.0', m_type, 1, CONCAT(t_name, '审批流程(通用四环节)'));
        SET tid = LAST_INSERT_ID();
        INSERT INTO approval_process_node (template_id, node_name, node_type, assignee_role, sort_order, next_node_id)
        VALUES (tid, '受理', 'START', 'APPROVER', 1, NULL),
               (tid, '初审', 'APPROVAL', 'APPROVER', 2, NULL),
               (tid, '决定', 'APPROVAL', 'DECIDE', 3, NULL),
               (tid, '办结', 'END', 'ISSUE', 4, NULL);
        SELECT id INTO n1 FROM approval_process_node WHERE template_id = tid AND sort_order = 1;
        SELECT id INTO n2 FROM approval_process_node WHERE template_id = tid AND sort_order = 2;
        SELECT id INTO n3 FROM approval_process_node WHERE template_id = tid AND sort_order = 3;
        SELECT id INTO n4 FROM approval_process_node WHERE template_id = tid AND sort_order = 4;
        UPDATE approval_process_node SET next_node_id = n2 WHERE id = n1;
        UPDATE approval_process_node SET next_node_id = n3 WHERE id = n2;
        UPDATE approval_process_node SET next_node_id = n4 WHERE id = n3;
    END IF;
END$$
DELIMITER ;

CALL add_citizen_template_if_missing('GA-HK-001', '户口迁移办理', 'GA-HK-001');
CALL add_citizen_template_if_missing('SB-JY-001', '社保关系转移', 'SB-JY-001');
CALL add_citizen_template_if_missing('YL-BX-001', '医保报销', 'YL-BX-001');

DROP PROCEDURE IF EXISTS add_citizen_template_if_missing;
