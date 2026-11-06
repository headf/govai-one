-- ============================================================
-- GovAI One 智慧政务系统 - 监管端 (govai-supervision)
-- 数据库: govai_supervision
-- 说明:   15 张表 + 预置数据。执行: mysql -uroot -proot123 < init.sql
-- ============================================================

CREATE DATABASE IF NOT EXISTS govai_supervision DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE govai_supervision;

-- ------------------------------------------------------------
-- 1. 系统用户表（全组登录用这张表，SSO 主表）
-- ------------------------------------------------------------
DROP TABLE IF EXISTS sys_user;
CREATE TABLE sys_user (
    id          BIGINT       PRIMARY KEY AUTO_INCREMENT,
    username    VARCHAR(50)  UNIQUE NOT NULL,
    password    VARCHAR(100) NOT NULL COMMENT 'BCrypt加密',
    real_name   VARCHAR(50),
    phone       VARCHAR(20),
    email       VARCHAR(100),
    department  VARCHAR(100),
    role_id     BIGINT,
    status      TINYINT      DEFAULT 1 COMMENT '1正常 0禁用',
    create_time DATETIME     DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统用户表';

-- ------------------------------------------------------------
-- 2. 角色表
-- ------------------------------------------------------------
DROP TABLE IF EXISTS sys_role;
CREATE TABLE sys_role (
    id          BIGINT       PRIMARY KEY AUTO_INCREMENT,
    role_name   VARCHAR(50)  NOT NULL,
    role_code   VARCHAR(30)  UNIQUE,
    description VARCHAR(200),
    status      TINYINT      DEFAULT 1,
    create_time DATETIME     DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色表';

-- ------------------------------------------------------------
-- 3. 权限表
-- ------------------------------------------------------------
DROP TABLE IF EXISTS sys_permission;
CREATE TABLE sys_permission (
    id              BIGINT       PRIMARY KEY AUTO_INCREMENT,
    permission_name VARCHAR(100) NOT NULL,
    permission_code VARCHAR(50)  UNIQUE,
    type            VARCHAR(20)  COMMENT 'MENU/BUTTON/API',
    parent_id       BIGINT       DEFAULT 0,
    sort_order      INT          DEFAULT 0,
    path            VARCHAR(200),
    create_time     DATETIME     DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='权限表';

-- ------------------------------------------------------------
-- 4. 角色权限关联表
-- ------------------------------------------------------------
DROP TABLE IF EXISTS sys_role_permission;
CREATE TABLE sys_role_permission (
    role_id       BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    PRIMARY KEY (role_id, permission_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色权限关联表';

-- ------------------------------------------------------------
-- 5. 企业信用评价表
-- ------------------------------------------------------------
DROP TABLE IF EXISTS credit_enterprise;
CREATE TABLE credit_enterprise (
    id               BIGINT       PRIMARY KEY AUTO_INCREMENT,
    enterprise_id    BIGINT       NOT NULL,
    credit_score     DECIMAL(5,2) DEFAULT 100,
    credit_level     VARCHAR(10)  COMMENT 'A/B/C/D',
    dimension_scores TEXT         COMMENT '各维度得分(JSON)',
    update_time      DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_enterprise (enterprise_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='企业信用评价表';

-- ------------------------------------------------------------
-- 6. 个人信用评价表
-- ------------------------------------------------------------
DROP TABLE IF EXISTS credit_personal;
CREATE TABLE credit_personal (
    id           BIGINT       PRIMARY KEY AUTO_INCREMENT,
    credit_no    VARCHAR(32)  COMMENT '个人信用单号(CX+日期+随机)',
    user_id      BIGINT       NOT NULL,
    credit_score DECIMAL(5,2) DEFAULT 100,
    credit_level VARCHAR(10),
    update_time  DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='个人信用评价表';

-- ------------------------------------------------------------
-- 7. 信用修复表
-- ------------------------------------------------------------
DROP TABLE IF EXISTS credit_repair;
CREATE TABLE credit_repair (
    id             BIGINT       PRIMARY KEY AUTO_INCREMENT,
    repair_no      VARCHAR(32)  COMMENT '修复单号(XF+日期+随机)',
    target_type    VARCHAR(10)  COMMENT 'ENTERPRISE/PERSONAL',
    target_id      BIGINT       NOT NULL,
    repair_reason  VARCHAR(500),
    apply_time     DATETIME     DEFAULT CURRENT_TIMESTAMP,
    audit_status   VARCHAR(20)  DEFAULT 'PENDING',
    audit_opinion  VARCHAR(500),
    audit_time     DATETIME
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='信用修复申请表';

-- ------------------------------------------------------------
-- 8. 风险预警规则表
-- ------------------------------------------------------------
DROP TABLE IF EXISTS risk_rule;
CREATE TABLE risk_rule (
    id              BIGINT       PRIMARY KEY AUTO_INCREMENT,
    rule_name       VARCHAR(100) NOT NULL,
    rule_type       VARCHAR(30)  COMMENT 'TIMEOUT/ANOMALY/THRESHOLD',
    threshold_value VARCHAR(100),
    alert_level     VARCHAR(10)  COMMENT 'LOW/MEDIUM/HIGH',
    notify_type     VARCHAR(30)  COMMENT 'SMS/EMAIL/SITE',
    status          TINYINT      DEFAULT 1,
    create_time     DATETIME     DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='风险预警规则表';

-- ------------------------------------------------------------
-- 9. 风险预警记录表
-- ------------------------------------------------------------
DROP TABLE IF EXISTS risk_alert;
CREATE TABLE risk_alert (
    id            BIGINT       PRIMARY KEY AUTO_INCREMENT,
    alert_no      VARCHAR(32)  COMMENT '预警编号(YJ+日期+随机)',
    rule_id       BIGINT,
    target_type   VARCHAR(10),
    target_id     BIGINT,
    alert_level   VARCHAR(10),
    alert_content VARCHAR(500),
    alert_time    DATETIME     DEFAULT CURRENT_TIMESTAMP,
    handle_status VARCHAR(20)  DEFAULT 'PENDING',
    handle_time   DATETIME,
    INDEX idx_level_time (alert_level, alert_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='风险预警记录表';

-- ------------------------------------------------------------
-- 10. 抽样检查记录表
-- ------------------------------------------------------------
DROP TABLE IF EXISTS supervision_sample;
CREATE TABLE supervision_sample (
    id           BIGINT       PRIMARY KEY AUTO_INCREMENT,
    sample_no    VARCHAR(32)  COMMENT '抽查单号(CJ+日期+随机)',
    instance_id  BIGINT,
    sample_type  VARCHAR(30)  COMMENT 'RANDOM/TARGETED',
    check_result VARCHAR(20)  COMMENT 'PASS/FAIL',
    check_detail TEXT,
    checker_id   BIGINT,
    check_time   DATETIME     DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='抽样检查记录表';

-- ------------------------------------------------------------
-- 11. 审计追溯记录表
-- ------------------------------------------------------------
DROP TABLE IF EXISTS audit_trace;
CREATE TABLE audit_trace (
    id           BIGINT       PRIMARY KEY AUTO_INCREMENT,
    instance_id  BIGINT,
    trace_data   TEXT         COMMENT '全链路数据(JSON)',
    create_time  DATETIME     DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_instance (instance_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审计追溯记录表';

-- ------------------------------------------------------------
-- 12. 日统计汇总表
-- ------------------------------------------------------------
DROP TABLE IF EXISTS stats_daily;
CREATE TABLE stats_daily (
    id                 BIGINT        PRIMARY KEY AUTO_INCREMENT,
    stat_date          DATE          UNIQUE,
    total_applications INT           DEFAULT 0,
    approved_count     INT           DEFAULT 0,
    rejected_count     INT           DEFAULT 0,
    avg_process_hours  DECIMAL(10,2),
    timeout_count      INT           DEFAULT 0,
    satisfaction_avg   DECIMAL(3,2),
    create_time        DATETIME      DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_date (stat_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='日统计汇总表';

-- ------------------------------------------------------------
-- 13. 数据字典表
-- ------------------------------------------------------------
DROP TABLE IF EXISTS sys_dict;
CREATE TABLE sys_dict (
    id          BIGINT       PRIMARY KEY AUTO_INCREMENT,
    dict_type   VARCHAR(50)  NOT NULL,
    dict_label  VARCHAR(100) NOT NULL,
    dict_value  VARCHAR(100) NOT NULL,
    sort_order  INT          DEFAULT 0,
    status      TINYINT      DEFAULT 1,
    create_time DATETIME     DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_type (dict_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数据字典表';

-- ------------------------------------------------------------
-- 14. 系统操作日志表
-- ------------------------------------------------------------
DROP TABLE IF EXISTS sys_log;
CREATE TABLE sys_log (
    id          BIGINT       PRIMARY KEY AUTO_INCREMENT,
    user_id     BIGINT,
    username    VARCHAR(50),
    operation   VARCHAR(200),
    method      VARCHAR(200),
    params      TEXT,
    ip          VARCHAR(50),
    cost_time   BIGINT COMMENT '耗时ms',
    create_time DATETIME     DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_time (user_id, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统操作日志表';

-- ------------------------------------------------------------
-- 15. 审批数据缓存表（RabbitMQ 接收审批端推送数据）
-- ------------------------------------------------------------
DROP TABLE IF EXISTS approval_data_cache;
CREATE TABLE approval_data_cache (
    id                   BIGINT        PRIMARY KEY AUTO_INCREMENT,
    instance_id          BIGINT        NOT NULL,
    matter_name          VARCHAR(200),
    applicant_name       VARCHAR(50),
    applicant_type       VARCHAR(20),
    approval_status      VARCHAR(20),
    submit_time          DATETIME,
    finish_time          DATETIME,
    process_duration_hours DECIMAL(10,2),
    receive_time         DATETIME      DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_instance (instance_id),
    INDEX idx_status (approval_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审批数据缓存表(接收MQ推送)';

-- ============================================================
-- 预置数据
-- ============================================================

-- 角色
INSERT INTO sys_role (role_name, role_code, description) VALUES
('超级管理员', 'ADMIN',      '系统全部权限'),
('审批员',     'APPROVER',   '审批办件'),
('监管员',     'SUPERVISOR', '监督管理'),
('群众',       'CITIZEN',    '群众端用户'),
('企业经办人', 'ENTERPRISE', '企业端用户');

-- 用户（密码均为 123456，BCrypt，与 Spring Security 兼容）
-- admin / approver / supervisor / supervisor1 可直接登录监管端
INSERT INTO sys_user (username, password, real_name, phone, department, role_id, status) VALUES
('admin',       '$2a$10$gvai4/0LPs5a3eKHFhu0nOMZurDYVzGzhfVyTx6cwdVWqCSMIs0UG', '系统管理员', '13800000000', '信息中心', 1, 1),
('approver1',   '$2a$10$gvai4/0LPs5a3eKHFhu0nOMZurDYVzGzhfVyTx6cwdVWqCSMIs0UG', '张审批',     '13800000001', '审批科',   2, 1),
('supervisor1', '$2a$10$gvai4/0LPs5a3eKHFhu0nOMZurDYVzGzhfVyTx6cwdVWqCSMIs0UG', '李监管',     '13800000002', '督查科',   3, 1);

-- 权限样例（监管后台菜单）
INSERT INTO sys_permission (permission_name, permission_code, type, parent_id, sort_order, path) VALUES
('首页监控', 'dashboard', 'MENU', 0, 1, '/dashboard'),
('用户管理', 'user:list', 'MENU', 0, 2, '/system/user'),
('角色管理', 'role:list', 'MENU', 0, 3, '/system/role'),
('信用管理', 'credit:list', 'MENU', 0, 4, '/credit'),
('风险预警', 'risk:list', 'MENU', 0, 5, '/risk'),
('抽样检查', 'sample:list', 'MENU', 0, 6, '/sample'),
('审计追溯', 'audit:list', 'MENU', 0, 7, '/audit'),
('统计报表', 'stats:view', 'MENU', 0, 8, '/stats');

-- 数据字典基础数据
INSERT INTO sys_dict (dict_type, dict_label, dict_value, sort_order) VALUES
('credit_level', 'A(优秀)', 'A', 1),
('credit_level', 'B(良好)', 'B', 2),
('credit_level', 'C(一般)', 'C', 3),
('credit_level', 'D(较差)', 'D', 4),
('alert_level', '低风险', 'LOW', 1),
('alert_level', '中风险', 'MEDIUM', 2),
('alert_level', '高风险', 'HIGH', 3),
('alert_status', '待处理', 'PENDING', 1),
('alert_status', '已处理', 'HANDLED', 2);

-- 基础风险预警规则样例
INSERT INTO risk_rule (rule_name, rule_type, threshold_value, alert_level, notify_type, status) VALUES
('超期未办结', 'TIMEOUT', '', 'HIGH', 'SITE', 1),
('申请量突增', 'THRESHOLD', '50', 'MEDIUM', 'SITE', 1),
('满意度低于阈值', 'THRESHOLD', '3', 'MEDIUM', 'SITE', 1);

-- 日统计表 stats_daily 仅建表（时序按真实办件实时聚合，不再预置样例数据）