-- GovAI 审批端: 按企业端事项编码批量补流程模板(幂等)
-- 目的: 企业端(ent_matter_info 18个 matterCode)申报时以 matterCode 作 templateCode 提交审批端,
--       审批端须有对应模板才能真实流转,否则 5001 本地降级。此处为每个编码补一套 受理→初审→决定→办结 通用模板。
-- 仅当编码不存在时插入,不删不改已有模板/实例。

USE govai_approval;

DROP PROCEDURE IF EXISTS add_template_if_missing;
DELIMITER $$
CREATE PROCEDURE add_template_if_missing(IN t_code VARCHAR(64), IN t_name VARCHAR(128), IN m_type VARCHAR(64))
BEGIN
    DECLARE tid BIGINT;
    DECLARE n1,n2,n3,n4 BIGINT;
    IF NOT EXISTS (SELECT 1 FROM approval_process_template WHERE template_code = t_code) THEN
        INSERT INTO approval_process_template (template_code, template_name, version, matter_type, status, description)
        VALUES (t_code, t_name, '1.0', m_type, 1, CONCAT(t_name, '审批流程(通用四环节)'));
        SET tid = LAST_INSERT_ID();
        -- 4 节点: 受理(START) 初审(APPROVAL) 决定(APPROVAL) 办结(END), 按 sort_order 建后再串 next_node_id
        INSERT INTO approval_process_node (template_id, node_name, node_type, assignee_role, sort_order, next_node_id)
        VALUES (tid, '受理', 'START', 'APPROVER', 1, NULL),
               (tid, '初审', 'APPROVAL', 'APPROVER', 2, NULL),
               (tid, '决定', 'APPROVAL', 'SUPERVISOR', 3, NULL),
               (tid, '办结', 'END', 'APPROVER', 4, NULL);
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

CALL add_template_if_missing('QY-KB-001', '企业开办一件事(登记)', 'QY-KB-001');
CALL add_template_if_missing('SP-JY-001', '食品经营许可新办', 'SP-JY-001');
CALL add_template_if_missing('ZZ-GX-001', '高新技术企业认定', 'ZZ-GX-001');
CALL add_template_if_missing('SP-BZ-001', '小规模纳税人增值税减免确认', 'SP-BZ-001');
CALL add_template_if_missing('SP-JY-002', '食品经营许可变更', 'SP-JY-002');
CALL add_template_if_missing('SP-JY-003', '食品经营许可延续', 'SP-JY-003');
CALL add_template_if_missing('SP-JY-004', '食品经营许可注销', 'SP-JY-004');
CALL add_template_if_missing('QY-KB-002', '企业变更一件事', 'QY-KB-002');
CALL add_template_if_missing('QY-KB-003', '企业简易注销一件事', 'QY-KB-003');
CALL add_template_if_missing('SP-BZ-002', '餐饮业稳岗返还补贴申报', 'SP-BZ-002');
CALL add_template_if_missing('WS-WS-001', '公共场所卫生许可新办', 'WS-WS-001');
CALL add_template_if_missing('WS-WS-002', '公共场所卫生许可延续', 'WS-WS-002');
CALL add_template_if_missing('SP-JY-005', '食品经营许可补办', 'SP-JY-005');
CALL add_template_if_missing('YC-ZM-001', '烟草专卖零售许可新办', 'YC-ZM-001');
CALL add_template_if_missing('YP-JY-001', '药品经营许可新办', 'YP-JY-001');
CALL add_template_if_missing('PW-PX-001', '排污许可新办', 'PW-PX-001');
CALL add_template_if_missing('QY-KB-004', '股权变更登记', 'QY-KB-004');
CALL add_template_if_missing('QY-KB-005', '经营期限延期', 'QY-KB-005');

DROP PROCEDURE IF EXISTS add_template_if_missing;
