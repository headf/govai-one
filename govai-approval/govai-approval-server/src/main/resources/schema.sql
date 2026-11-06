-- GovAI One 审批员端数据库建表脚本
-- 数据库：govai_approval
-- 对应 Member A 的 9 张核心表

CREATE DATABASE IF NOT EXISTS govai_approval DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE govai_approval;

-- 1. 流程模板表
CREATE TABLE IF NOT EXISTS approval_process_template (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    template_code VARCHAR(64) NOT NULL COMMENT '模板编码',
    template_name VARCHAR(128) NOT NULL COMMENT '模板名称',
    version     VARCHAR(16)  DEFAULT '1.0' COMMENT '版本号',
    matter_type VARCHAR(64)  COMMENT '事项类型',
    status      INT          DEFAULT 1 COMMENT '1启用 0停用',
    node_config TEXT         COMMENT '环节配置JSON(冗余)',
    description VARCHAR(512) COMMENT '描述',
    create_time DATETIME     DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted     INT          DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_template_code (template_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='流程模板';

-- 2. 流程环节表
CREATE TABLE IF NOT EXISTS approval_process_node (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    template_id  BIGINT       NOT NULL COMMENT '所属模板',
    node_name    VARCHAR(128) NOT NULL COMMENT '环节名称',
    node_type    VARCHAR(32)  DEFAULT 'APPROVAL' COMMENT 'START/APPROVAL/RULE_CHECK/BRANCH/END',
    assignee_role VARCHAR(64) COMMENT '办理人角色',
    sort_order   INT          DEFAULT 1 COMMENT '排序',
    next_node_id BIGINT       COMMENT '下一环节ID',
    branch_config TEXT        COMMENT '分支配置JSON',
    PRIMARY KEY (id),
    KEY idx_template_id (template_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='流程环节';

-- 3. 审批实例表
CREATE TABLE IF NOT EXISTS approval_instance (
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    template_id    BIGINT       COMMENT '流程模板ID',
    matter_id      BIGINT       COMMENT '事项ID',
    matter_name    VARCHAR(256) COMMENT '事项名称',
    applicant_name VARCHAR(128) COMMENT '申请人',
    applicant_id   VARCHAR(64)  COMMENT '申请人证件号',
    applicant_type VARCHAR(16)  COMMENT 'PERSONAL/ENTERPRISE',
    form_data      TEXT         COMMENT '申报表单数据JSON',
    status         VARCHAR(16)  DEFAULT 'PENDING' COMMENT 'PENDING/APPROVING/APPROVED/REJECTED/RETURNED',
    submit_time    DATETIME     COMMENT '提交时间',
    current_node_id BIGINT      COMMENT '当前环节ID',
    finish_time    DATETIME     COMMENT '办结时间',
    create_time    DATETIME     DEFAULT CURRENT_TIMESTAMP,
    deleted        INT          DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_status (status),
    KEY idx_applicant (applicant_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审批实例';

-- 4. 审批任务表
CREATE TABLE IF NOT EXISTS approval_task (
    id                 BIGINT       NOT NULL AUTO_INCREMENT,
    instance_id        BIGINT       NOT NULL COMMENT '审批实例ID',
    node_id            BIGINT       COMMENT '环节ID',
    assignee_user_id   BIGINT       COMMENT '办理人ID',
    assignee_user_name VARCHAR(128) COMMENT '办理人姓名',
    task_status        VARCHAR(16)  DEFAULT 'PENDING' COMMENT 'PENDING/DONE/CANCEL',
    approve_result     VARCHAR(16)  COMMENT 'PASS/REJECT/RETURN',
    approve_opinion    VARCHAR(512) COMMENT '审批意见',
    create_time        DATETIME     DEFAULT CURRENT_TIMESTAMP,
    handle_time        DATETIME     COMMENT '处理时间',
    delegate_to        BIGINT       COMMENT '委托给谁',
    PRIMARY KEY (id),
    KEY idx_instance (instance_id),
    KEY idx_assignee (assignee_user_id),
    KEY idx_status (task_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审批任务';

-- 5. 规则集表
CREATE TABLE IF NOT EXISTS approval_rule_set (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    rule_set_name VARCHAR(128) NOT NULL COMMENT '规则集名称',
    matter_type  VARCHAR(64)  COMMENT '事项类型',
    status       INT          DEFAULT 1 COMMENT '1启用 0停用',
    logic_type   VARCHAR(8)   DEFAULT 'AND' COMMENT 'AND/OR',
    description  VARCHAR(512) COMMENT '描述',
    create_time  DATETIME     DEFAULT CURRENT_TIMESTAMP,
    deleted      INT          DEFAULT 0,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='规则集';

-- 6. 规则项表
CREATE TABLE IF NOT EXISTS approval_rule_item (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    rule_set_id   BIGINT       NOT NULL COMMENT '所属规则集',
    rule_type     VARCHAR(16)  NOT NULL COMMENT 'NUMBER/TEXT/DATE/LICENSE',
    field_name    VARCHAR(64)  NOT NULL COMMENT '表单字段名',
    operator      VARCHAR(16)  COMMENT 'GT/LT/EQ/CONTAINS/BETWEEN',
    compare_value VARCHAR(256) COMMENT '比较值',
    logic_operator VARCHAR(8)  DEFAULT 'AND' COMMENT '与上一条的 AND/OR',
    sort_order    INT          DEFAULT 1,
    error_message VARCHAR(256) COMMENT '不通过时提示信息',
    PRIMARY KEY (id),
    KEY idx_rule_set (rule_set_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='规则项';

-- 7. 审计日志表（不可修改）
CREATE TABLE IF NOT EXISTS audit_log (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    instance_id     BIGINT       COMMENT '审批实例ID',
    operator_id     BIGINT       COMMENT '操作人ID(AI为null)',
    operator_name   VARCHAR(128) COMMENT '操作人姓名',
    operation_type  VARCHAR(16)  COMMENT 'SUBMIT/RECEIVE/APPROVE/RETURN/FINISH/AI_ADVICE',
    operation_opinion VARCHAR(512) COMMENT '操作意见',
    before_status   VARCHAR(16)  COMMENT '操作前状态',
    after_status    VARCHAR(16)  COMMENT '操作后状态',
    operation_time  DATETIME     DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_instance (instance_id),
    KEY idx_operator (operator_id),
    KEY idx_op_time (operation_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审计日志(不可修改)';

-- 8. AI Prompt 模板表
CREATE TABLE IF NOT EXISTS ai_prompt_template (
    id                  BIGINT       NOT NULL AUTO_INCREMENT,
    template_name       VARCHAR(128) NOT NULL COMMENT '模板名称',
    scene_type          VARCHAR(32)  COMMENT 'OPINION/POLICY_SUMMARY/RISK',
    system_prompt       TEXT         COMMENT '系统提示词',
    user_prompt_template TEXT        COMMENT '用户提示词模板({变量})',
    variables           VARCHAR(512) COMMENT '变量列表JSON',
    status              INT          DEFAULT 1,
    create_time         DATETIME     DEFAULT CURRENT_TIMESTAMP,
    deleted             INT          DEFAULT 0,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI Prompt模板';

-- 9. AI 对话记录表
CREATE TABLE IF NOT EXISTS ai_chat_record (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    instance_id     BIGINT       COMMENT '关联审批实例',
    template_id     BIGINT       COMMENT '关联Prompt模板',
    user_message    TEXT         COMMENT '用户消息',
    ai_response     TEXT         COMMENT 'AI响应',
    token_count     INT          COMMENT 'Token消耗',
    response_time_ms BIGINT      COMMENT '响应耗时ms',
    create_time     DATETIME     DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_instance (instance_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI对话记录';

-- ============== 初始化测试数据 ==============

-- 流程模板：新办企业审批
INSERT INTO approval_process_template (template_code, template_name, version, matter_type, status, description)
VALUES ('ENTERPRISE_REGISTER', '新办企业审批流程', '1.0', 'ENTERPRISE_REGISTER', 1, '企业开办一件事审批流程');

INSERT INTO approval_process_node (template_id, node_name, node_type, assignee_role, sort_order, next_node_id)
SELECT id, '受理', 'START', 'APPROVER', 1, NULL FROM approval_process_template WHERE template_code='ENTERPRISE_REGISTER';
INSERT INTO approval_process_node (template_id, node_name, node_type, assignee_role, sort_order, next_node_id)
SELECT id, '初审', 'APPROVAL', 'APPROVER', 2, NULL FROM approval_process_template WHERE template_code='ENTERPRISE_REGISTER';
INSERT INTO approval_process_node (template_id, node_name, node_type, assignee_role, sort_order, next_node_id)
SELECT id, '决定', 'APPROVAL', 'SUPERVISOR', 3, NULL FROM approval_process_template WHERE template_code='ENTERPRISE_REGISTER';
INSERT INTO approval_process_node (template_id, node_name, node_type, assignee_role, sort_order, next_node_id)
SELECT id, '办结', 'END', 'APPROVER', 4, NULL FROM approval_process_template WHERE template_code='ENTERPRISE_REGISTER';

-- 串联 next_node_id
UPDATE approval_process_node n1
INNER JOIN approval_process_node n2 ON n1.template_id = n2.template_id AND n2.sort_order = 2
SET n1.next_node_id = n2.id WHERE n1.sort_order = 1 AND n1.template_id = (SELECT id FROM approval_process_template WHERE template_code='ENTERPRISE_REGISTER');
UPDATE approval_process_node n1
INNER JOIN approval_process_node n2 ON n1.template_id = n2.template_id AND n2.sort_order = 3
SET n1.next_node_id = n2.id WHERE n1.sort_order = 2 AND n1.template_id = (SELECT id FROM approval_process_template WHERE template_code='ENTERPRISE_REGISTER');
UPDATE approval_process_node n1
INNER JOIN approval_process_node n2 ON n1.template_id = n2.template_id AND n2.sort_order = 4
SET n1.next_node_id = n2.id WHERE n1.sort_order = 3 AND n1.template_id = (SELECT id FROM approval_process_template WHERE template_code='ENTERPRISE_REGISTER');

-- 规则集
INSERT INTO approval_rule_set (rule_set_name, matter_type, status, logic_type, description)
VALUES ('新办企业基本规则', 'ENTERPRISE_REGISTER', 1, 'AND', '新办企业审批基础规则集');

INSERT INTO approval_rule_item (rule_set_id, rule_type, field_name, operator, compare_value, logic_operator, sort_order, error_message)
SELECT id, 'TEXT', 'businessType', 'CONTAINS', '餐饮', 'AND', 1, '业务类型不匹配' FROM approval_rule_set WHERE rule_set_name='新办企业基本规则';
INSERT INTO approval_rule_item (rule_set_id, rule_type, field_name, operator, compare_value, logic_operator, sort_order, error_message)
SELECT id, 'LICENSE', 'businessLicense', 'EQ', 'true', 'AND', 2, '营业执照未提供' FROM approval_rule_set WHERE rule_set_name='新办企业基本规则';

-- Prompt 模板
INSERT INTO ai_prompt_template (template_name, scene_type, system_prompt, user_prompt_template, status)
VALUES ('审批意见生成模板', 'OPINION', '你是政务审批辅助AI。输出必须为纯文本，禁止使用 Markdown 符号（#、**、*、`、表格|、---）、emoji 或特殊符号；仅能引用申报表单与规则校验结果中真实存在的信息，不得编造表单外内容；通过/不通过/是否办结由系统规则与人工决定，你只输出风险与核查提示，不输出结论或建议决定。',
        '申报事项：{matterName}\n申请人：{applicantName}\n申报表单：{formData}\n规则引擎逐项校验结果：\n{facts}\n请据此输出简要的核查要点与风险提示（纯文本）。不要重复申报事项、申请人，不要给出通过/不通过等结论。', 1);

INSERT INTO ai_prompt_template (template_name, scene_type, system_prompt, user_prompt_template, status)
VALUES ('政策摘要模板', 'POLICY_SUMMARY', '你是政策解读AI，请将政策内容归纳为简明摘要。',
        '请为以下内容生成摘要：{message}', 1);

-- 测试审批实例
INSERT INTO approval_instance (template_id, matter_id, matter_name, applicant_name, applicant_id, applicant_type, form_data, status, submit_time, current_node_id, create_time)
SELECT t.id, 1, '餐饮公司设立登记', '张三', '330102199001011234', 'ENTERPRISE',
       '{"businessType":"餐饮","capital":"50万","address":"杭州市西湖区","businessLicense":"true"}',
       'APPROVING', NOW(), NULL, NOW()
FROM approval_process_template t WHERE t.template_code='ENTERPRISE_REGISTER';

-- 将实例当前环节设为「初审」（第2个环节），使「受理」显示为已完成
UPDATE approval_instance inst
INNER JOIN approval_process_node n ON n.template_id = inst.template_id AND n.sort_order = 2
SET inst.current_node_id = n.id
WHERE inst.matter_name = '餐饮公司设立登记';

-- 补充审批任务记录，使进度和操作日志有实际办理数据
INSERT INTO approval_task (instance_id, node_id, task_status, assignee_user_name, approve_result, approve_opinion, create_time, handle_time)
SELECT inst.id, n1.id, 'DONE', '李审批员', 'PASS', '材料齐全，符合受理条件', NOW(), NOW()
FROM approval_instance inst
INNER JOIN approval_process_node n1 ON n1.template_id = inst.template_id AND n1.sort_order = 1
WHERE inst.matter_name = '餐饮公司设立登记';

INSERT INTO approval_task (instance_id, node_id, task_status, assignee_user_name, create_time)
SELECT inst.id, n2.id, 'PENDING', '王审批员', NOW()
FROM approval_instance inst
INNER JOIN approval_process_node n2 ON n2.template_id = inst.template_id AND n2.sort_order = 2
WHERE inst.matter_name = '餐饮公司设立登记';
