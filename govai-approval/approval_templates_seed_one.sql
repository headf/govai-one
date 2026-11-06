-- GovAI 审批端: 按企业端事项编码批量补流程模板(幂等,可重复执行)
-- 仅当编码不存在时插入模板+4节点;已有模板/实例不动。

USE govai_approval;

-- 企业开办一件事(登记) QY-KB-001
INSERT INTO approval_process_template (template_code, template_name, version, matter_type, status, description)
SELECT 'QY-KB-001', '企业开办一件事(登记)', '1.0', 'QY-KB-001', 1, '企业开办一件事审批流程(通用四环节)'
WHERE NOT EXISTS (SELECT 1 FROM approval_process_template WHERE template_code = 'QY-KB-001');
SET @tid = (SELECT id FROM approval_process_template WHERE template_code = 'QY-KB-001');
INSERT INTO approval_process_node (template_id, node_name, node_type, assignee_role, sort_order)
SELECT @tid, '受理', 'START', 'APPROVER', 1 WHERE NOT EXISTS (SELECT 1 FROM approval_process_node WHERE template_id = @tid AND sort_order = 1);
INSERT INTO approval_process_node (template_id, node_name, node_type, assignee_role, sort_order)
SELECT @tid, '初审', 'APPROVAL', 'APPROVER', 2 WHERE NOT EXISTS (SELECT 1 FROM approval_process_node WHERE template_id = @tid AND sort_order = 2);
INSERT INTO approval_process_node (template_id, node_name, node_type, assignee_role, sort_order)
SELECT @tid, '决定', 'APPROVAL', 'SUPERVISOR', 3 WHERE NOT EXISTS (SELECT 1 FROM approval_process_node WHERE template_id = @tid AND sort_order = 3);
INSERT INTO approval_process_node (template_id, node_name, node_type, assignee_role, sort_order)
SELECT @tid, '办结', 'END', 'APPROVER', 4 WHERE NOT EXISTS (SELECT 1 FROM approval_process_node WHERE template_id = @tid AND sort_order = 4);
UPDATE approval_process_node a JOIN approval_process_node b ON b.template_id = @tid AND b.sort_order = 2 SET a.next_node_id = b.id WHERE a.template_id = @tid AND a.sort_order = 1;
UPDATE approval_process_node a JOIN approval_process_node b ON b.template_id = @tid AND b.sort_order = 3 SET a.next_node_id = b.id WHERE a.template_id = @tid AND a.sort_order = 2;
UPDATE approval_process_node a JOIN approval_process_node b ON b.template_id = @tid AND b.sort_order = 4 SET a.next_node_id = b.id WHERE a.template_id = @tid AND a.sort_order = 3;
