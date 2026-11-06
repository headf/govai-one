-- =========================================================
-- GovAI One 智慧政务系统 - 群众端 (govai_citizen) 建库建表脚本
-- 负责人: 张梦凡 | 数据库: govai_citizen | MySQL 8.0
-- =========================================================
CREATE DATABASE IF NOT EXISTS govai_citizen DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE govai_citizen;

-- 1. 群众用户表
CREATE TABLE IF NOT EXISTS citizen_user (
    id              BIGINT       PRIMARY KEY AUTO_INCREMENT,
    openid          VARCHAR(64)  UNIQUE COMMENT '微信openid',
    phone           VARCHAR(20)  COMMENT '手机号',
    real_name       VARCHAR(50)  COMMENT '姓名',
    id_card         VARCHAR(18)  COMMENT '身份证号',
    avatar_url      VARCHAR(255) COMMENT '头像URL',
    status          TINYINT      DEFAULT 1 COMMENT '1正常 0禁用',
    create_time     DATETIME     DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_openid (openid),
    INDEX idx_phone (phone)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='群众用户表';

-- 2. 事项分类表
CREATE TABLE IF NOT EXISTS matter_category (
    id              BIGINT       PRIMARY KEY AUTO_INCREMENT,
    category_name   VARCHAR(100) NOT NULL COMMENT '分类名称',
    parent_id       BIGINT       DEFAULT 0 COMMENT '父分类ID',
    sort_order      INT          DEFAULT 0 COMMENT '排序',
    icon            VARCHAR(255) COMMENT '图标URL',
    create_time     DATETIME     DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='事项分类表';

-- 3. 事项信息表
CREATE TABLE IF NOT EXISTS matter_info (
    id              BIGINT       PRIMARY KEY AUTO_INCREMENT,
    matter_code     VARCHAR(50)  UNIQUE COMMENT '事项编码',
    matter_name     VARCHAR(200) NOT NULL COMMENT '事项名称',
    category_id     BIGINT       COMMENT '分类ID',
    department      VARCHAR(100) COMMENT '实施部门',
    description     TEXT         COMMENT '事项描述',
    guide_content   TEXT         COMMENT '办理指南(富文本)',
    conditions      TEXT         COMMENT '申报条件(JSON数组)',
    materials       TEXT         COMMENT '所需材料(JSON数组)',
    process_steps   TEXT         COMMENT '办理流程(JSON数组)',
    deadline_days   INT          DEFAULT 20 COMMENT '法定办结天数',
    status          TINYINT      DEFAULT 1 COMMENT '1上线 0下线',
    sort_order      INT          DEFAULT 0,
    create_time     DATETIME     DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_category (category_id),
    INDEX idx_code (matter_code),
    FULLTEXT INDEX ft_name (matter_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='事项信息表';

-- 4. 申报记录表
CREATE TABLE IF NOT EXISTS application_record (
    id              BIGINT       PRIMARY KEY AUTO_INCREMENT,
    record_code     VARCHAR(50)  UNIQUE COMMENT '申报编号',
    matter_id       BIGINT       NOT NULL COMMENT '事项ID',
    user_id         BIGINT       NOT NULL COMMENT '群众用户ID',
    applicant_name  VARCHAR(50)  COMMENT '申请人姓名',
    applicant_phone VARCHAR(20)  COMMENT '联系电话',
    applicant_id_card VARCHAR(18) COMMENT '身份证号',
    form_data       TEXT         COMMENT '表单数据(JSON)',
    status          VARCHAR(20)  DEFAULT 'PENDING' COMMENT 'PENDING受理中 APPROVING审批中 APPROVED已办结 REJECTED已驳回',
    approval_instance_id BIGINT  COMMENT '关联审批系统实例ID(跨服务)',
    submit_time     DATETIME     COMMENT '提交时间',
    finish_time     DATETIME     COMMENT '办结时间',
    create_time     DATETIME     DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user (user_id),
    INDEX idx_matter (matter_id),
    INDEX idx_status (status),
    INDEX idx_instance (approval_instance_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='申报记录表';

-- 5. 申报材料表
CREATE TABLE IF NOT EXISTS application_material (
    id              BIGINT       PRIMARY KEY AUTO_INCREMENT,
    record_id       BIGINT       NOT NULL COMMENT '申报记录ID',
    material_name   VARCHAR(100) COMMENT '材料名称',
    file_url        VARCHAR(500) COMMENT '文件URL(本地uploads/MinIO)',
    file_type       VARCHAR(20)  COMMENT '文件类型',
    ocr_result      TEXT         COMMENT 'OCR识别结果(JSON)',
    ocr_status      TINYINT      DEFAULT 0 COMMENT '0未识别 1已识别 2识别失败',
    upload_time     DATETIME     DEFAULT CURRENT_TIMESTAMP,
    create_time     DATETIME     DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_record (record_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='申报材料表';

-- 6. 进度通知表
CREATE TABLE IF NOT EXISTS progress_notification (
    id              BIGINT       PRIMARY KEY AUTO_INCREMENT,
    record_id       BIGINT       NOT NULL COMMENT '申报记录ID',
    node_name       VARCHAR(100) COMMENT '环节名称',
    content         VARCHAR(500) COMMENT '通知内容',
    notify_type     VARCHAR(20)  COMMENT 'WECHAT微信 SMS短信 SITE站内',
    send_status     TINYINT      DEFAULT 0 COMMENT '0未发 1已发 2失败',
    send_time       DATETIME     COMMENT '发送时间',
    create_time     DATETIME     DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_record (record_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='进度通知表';

-- 7. 站内消息表
CREATE TABLE IF NOT EXISTS site_message (
    id              BIGINT       PRIMARY KEY AUTO_INCREMENT,
    user_id         BIGINT       NOT NULL COMMENT '接收用户ID',
    message_type    VARCHAR(30)  COMMENT 'APPROVAL/SYSTEM/POLICY',
    title           VARCHAR(200) COMMENT '消息标题',
    content         TEXT         COMMENT '消息内容',
    record_id       BIGINT       COMMENT '关联申报记录ID',
    is_read         TINYINT      DEFAULT 0 COMMENT '0未读 1已读',
    read_time       DATETIME     COMMENT '阅读时间',
    create_time     DATETIME     DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_read (user_id, is_read)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='站内消息表';

-- 8. 好差评表
CREATE TABLE IF NOT EXISTS evaluation (
    id              BIGINT       PRIMARY KEY AUTO_INCREMENT,
    record_id       BIGINT       NOT NULL COMMENT '申报记录ID',
    user_id         BIGINT       NOT NULL COMMENT '评价人ID',
    star_rating     TINYINT      NOT NULL COMMENT '星级1-5',
    content         VARCHAR(500) COMMENT '文字评价',
    tags             VARCHAR(200) COMMENT '标签(JSON数组)',
    create_time     DATETIME     DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_record (record_id),
    INDEX idx_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='好差评表';

-- 9. 政策推荐记录表
CREATE TABLE IF NOT EXISTS policy_recommend (
    id              BIGINT       PRIMARY KEY AUTO_INCREMENT,
    user_id         BIGINT       NOT NULL COMMENT '用户ID',
    policy_title    VARCHAR(200) COMMENT '政策标题',
    policy_content  TEXT         COMMENT '政策内容',
    match_reason    VARCHAR(500) COMMENT '匹配原因',
    is_read         TINYINT      DEFAULT 0,
    create_time     DATETIME     DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='政策推荐记录表';

-- =========================================================
-- 预置数据
-- =========================================================
INSERT IGNORE INTO matter_category (category_name, sort_order) VALUES
('公安户籍', 1), ('社保就业', 2), ('医疗卫生', 3),
('教育文化', 4), ('民政救助', 5), ('住房保障', 6);

INSERT IGNORE INTO matter_info (matter_code, matter_name, category_id, department, description, conditions, materials, process_steps, deadline_days) VALUES
('GA-HK-001', '户口迁移办理', 1, '公安局', '办理户口迁移手续', '["本市户籍","有合法稳定住所"]', '["身份证","房产证或租赁合同","户口本"]', '["提交申请","审核材料","办理迁移"]', 15),
('SB-JY-001', '社保关系转移', 2, '人社局', '办理社保关系转移', '["已参保","有接收地"]', '["身份证","社保卡","参保凭证"]', '["提交申请","转出地审核","转入地接收"]', 20),
('YL-BX-001', '医保报销', 3, '医保局', '医疗费用医保报销', '["已参保医保","在定点医疗机构就诊"]', '["身份证","医保卡","医疗费用清单","诊断证明"]', '["提交材料","审核","报销"]', 30);
