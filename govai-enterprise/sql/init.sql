-- =============================================================
-- GovAI One 企业端(govai-enterprise) 建库建表脚本
-- 负责人:郝锦瑞 | 依据《GovAI-One-全组开发骨架文档》2.4 节
-- 库:govai_enterprise | 9 张表 + 种子数据
-- 执行:mysql -uroot -p123456 -h127.0.0.1 < init.sql
-- =============================================================
CREATE DATABASE IF NOT EXISTS govai_enterprise DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE govai_enterprise;

-- -------------------------------------------------------------
-- 1. 企业用户表(登录账号;ADMIN 可管理 OPERATOR)
-- -------------------------------------------------------------
CREATE TABLE enterprise_user (
    id          BIGINT       PRIMARY KEY AUTO_INCREMENT,
    username    VARCHAR(50)  UNIQUE NOT NULL,
    password    VARCHAR(100) COMMENT 'BCrypt',
    enterprise_id BIGINT,
    real_name   VARCHAR(50),
    phone       VARCHAR(20),
    role        VARCHAR(20)  DEFAULT 'OPERATOR' COMMENT 'ADMIN/OPERATOR',
    status      TINYINT      DEFAULT 1,
    create_time DATETIME     DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='企业用户表';

-- -------------------------------------------------------------
-- 2. 企业信息表
-- -------------------------------------------------------------
CREATE TABLE enterprise_info (
    id               BIGINT       PRIMARY KEY AUTO_INCREMENT,
    credit_code      VARCHAR(18)  UNIQUE COMMENT '统一社会信用代码',
    enterprise_name  VARCHAR(200) NOT NULL,
    legal_person     VARCHAR(50),
    register_address VARCHAR(500),
    contact_phone    VARCHAR(20),
    enterprise_type  VARCHAR(50),
    establish_date   DATE,
    status           TINYINT      DEFAULT 1,
    create_time      DATETIME     DEFAULT CURRENT_TIMESTAMP,
    update_time      DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='企业信息表';

-- -------------------------------------------------------------
-- 3. 法人事项信息表
-- 说明:相对骨架文档新增 form_config 列,用于申报页动态表单渲染(需求 7.2)
--       conditions/materials/process_steps 均为 JSON 数组文本
-- -------------------------------------------------------------
CREATE TABLE ent_matter_info (
    id            BIGINT       PRIMARY KEY AUTO_INCREMENT,
    matter_code   VARCHAR(50)  UNIQUE,
    matter_name   VARCHAR(200) NOT NULL,
    category_id   BIGINT,
    department    VARCHAR(100),
    description   TEXT,
    guide_content TEXT,
    conditions    TEXT COMMENT '申报条件(JSON数组)',
    materials     TEXT COMMENT '所需材料(JSON数组)',
    process_steps TEXT COMMENT '办理流程(JSON数组)',
    deadline_days INT          DEFAULT 20,
    form_config   TEXT COMMENT '申报表单字段配置(JSON,动态表单渲染)',
    status        TINYINT      DEFAULT 1,
    create_time   DATETIME     DEFAULT CURRENT_TIMESTAMP,
    FULLTEXT INDEX ft_name (matter_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='法人事项表';

-- -------------------------------------------------------------
-- 4. 企业申报记录表
-- -------------------------------------------------------------
CREATE TABLE ent_application_record (
    id                  BIGINT       PRIMARY KEY AUTO_INCREMENT,
    record_code         VARCHAR(50)  UNIQUE,
    enterprise_id       BIGINT       NOT NULL,
    matter_id           BIGINT       NOT NULL,
    operator_id         BIGINT,
    form_data           TEXT COMMENT '表单数据(JSON)',
    status              VARCHAR(20)  DEFAULT 'PENDING',
    approval_instance_id BIGINT      COMMENT '关联审批实例(跨服务)',
    submit_time         DATETIME,
    finish_time         DATETIME,
    create_time         DATETIME     DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_enterprise (enterprise_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='企业申报记录表';

-- -------------------------------------------------------------
-- 5. 企业申报材料表
-- -------------------------------------------------------------
CREATE TABLE ent_application_material (
    id            BIGINT       PRIMARY KEY AUTO_INCREMENT,
    record_id     BIGINT       NOT NULL,
    material_name VARCHAR(100),
    file_url      VARCHAR(500),
    file_type     VARCHAR(20),
    ocr_result    TEXT,
    ocr_status    TINYINT      DEFAULT 0,
    upload_time   DATETIME     DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_record (record_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='企业申报材料表';

-- -------------------------------------------------------------
-- 6. 企业电子证照表(免证办数据源 + 办结出证落点)
-- -------------------------------------------------------------
CREATE TABLE enterprise_license (
    id            BIGINT       PRIMARY KEY AUTO_INCREMENT,
    enterprise_id BIGINT       NOT NULL,
    license_type  VARCHAR(50)  COMMENT '营业执照/资质证书/许可证',
    license_no    VARCHAR(100),
    license_name  VARCHAR(200),
    issue_date    DATE,
    expiry_date   DATE,
    file_url      VARCHAR(500),
    status        TINYINT      DEFAULT 1 COMMENT '1有效 0失效',
    create_time   DATETIME     DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_enterprise (enterprise_id),
    INDEX idx_expiry (expiry_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='企业电子证照表';

-- -------------------------------------------------------------
-- 7. 合规预检记录表
-- -------------------------------------------------------------
CREATE TABLE ent_compliance_check (
    id          BIGINT       PRIMARY KEY AUTO_INCREMENT,
    record_id   BIGINT       NOT NULL,
    check_item  VARCHAR(200),
    check_result TINYINT     COMMENT '1通过 0不通过',
    check_detail VARCHAR(500),
    check_time  DATETIME     DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_record (record_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='合规预检记录表';

-- -------------------------------------------------------------
-- 8. 企业政策匹配表(免申即享确认闭环)
-- -------------------------------------------------------------
CREATE TABLE ent_policy_match (
    id             BIGINT       PRIMARY KEY AUTO_INCREMENT,
    enterprise_id  BIGINT       NOT NULL,
    policy_title   VARCHAR(200),
    policy_content TEXT,
    match_score    DECIMAL(5,2),
    is_applied     TINYINT      DEFAULT 0,
    apply_time     DATETIME,
    create_time    DATETIME     DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_enterprise (enterprise_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='企业政策匹配表';

-- -------------------------------------------------------------
-- 9. 法人事项分类表
-- -------------------------------------------------------------
CREATE TABLE ent_matter_category (
    id            BIGINT       PRIMARY KEY AUTO_INCREMENT,
    category_name VARCHAR(100) NOT NULL,
    parent_id     BIGINT       DEFAULT 0,
    sort_order    INT          DEFAULT 0,
    create_time   DATETIME     DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='法人事项分类表';

-- =============================================================
-- 种子数据(演示用;登录账号由后端 DataInitializer 幂等写入 BCrypt 密码)
-- =============================================================

-- 事项分类
INSERT INTO ent_matter_category (id, category_name, parent_id, sort_order) VALUES
(1, '企业开办', 0, 1),
(2, '经营许可', 0, 2),
(3, '资质认定', 0, 3),
(4, '扶持补贴', 0, 4);

-- 法人事项(演示主链路:企业开办一件事 / 食品经营许可新办)
INSERT INTO ent_matter_info
(id, matter_code, matter_name, category_id, department, description, guide_content,
 conditions, materials, process_steps, deadline_days, form_config, status) VALUES
(1, 'QY-KB-001', '企业开办一件事', 1, '市场监督管理局',
 '整合营业执照核发、印章刻制、税务登记、社保登记四环节，一次申报、并联办理。',
 '适用对象：拟在本市新设内资有限公司的申请人。办理时限：1个工作日(联办)。',
 '["已取得合法经营场所","拟任法定代表人无失信记录"]',
 '["企业设立登记申请书(OCR)","公司章程","股东(发起人)主体资格证明","住所(经营场所)使用证明"]',
 '["一表申报","材料审查","并联办理","执照送达"]', 1,
 '[{"key":"enterpriseName","label":"企业名称","type":"text","required":true},
   {"key":"creditCode","label":"统一社会信用代码","type":"text","required":true,"ocr":"BUSINESS_LICENSE"},
   {"key":"legalPerson","label":"法定代表人","type":"text","required":true,"ocr":"BUSINESS_LICENSE"},
   {"key":"registerCapital","label":"注册资本(万元)","type":"number","required":true},
   {"key":"registerAddress","label":"注册地址","type":"text","required":true,"ocr":"BUSINESS_LICENSE"},
   {"key":"businessScope","label":"经营范围","type":"textarea","required":true,"ocr":"BUSINESS_LICENSE"},
   {"key":"shareholders","label":"股东构成","type":"textarea","required":false},
   {"key":"contactPhone","label":"经办人联系电话","type":"text","required":true}]',
 1),
(2, 'SP-JY-001', '食品经营许可新办', 2, '市场监督管理局',
 '从事食品销售、餐饮服务的经营者，须取得食品经营许可证。',
 '适用对象：已取得营业执照、拟开展餐饮/食品销售的市场主体。办理时限：10个工作日。',
 '["已取得营业执照","经营场所卫生条件达标","从业人员持有有效健康证明"]',
 '["营业执照(电子证照,免证办)","法定代表人身份证(OCR)","从业人员健康证(OCR)","经营场所平面布局图","食品安全管理制度"]',
 '["提交申请","形式审查","现场核查(如需)","审批发证"]', 10,
 '[{"key":"enterpriseName","label":"企业名称","type":"text","required":true},
   {"key":"creditCode","label":"统一社会信用代码","type":"text","required":true,"ocr":"BUSINESS_LICENSE"},
   {"key":"legalPerson","label":"法定代表人","type":"text","required":true,"ocr":"BUSINESS_LICENSE"},
   {"key":"legalPersonIdCard","label":"法定代表人身份证号","type":"text","required":true,"ocr":"ID_CARD"},
   {"key":"operateAddress","label":"经营场所地址","type":"text","required":true},
   {"key":"operateArea","label":"经营场所面积(㎡)","type":"number","required":true},
   {"key":"foodCategory","label":"经营项目","type":"select","required":true,
     "options":["热食类食品制售","冷食类食品制售","预包装食品销售","散装食品销售","糕点类食品制售"]},
   {"key":"contactPhone","label":"联系人电话","type":"text","required":true}]',
 1),
(3, 'ZZ-GX-001', '高新技术企业认定', 3, '科学技术局',
 '对符合条件的企业进行国家高新技术企业认定推荐。',
 '适用对象：注册满一年、拥有核心自主知识产权、研发投入占比达标的企业。',
 '["成立满一年以上","拥有核心自主知识产权","研发费用占销售收入比例达标"]',
 '["营业执照(电子证照,免证办)","知识产权证书","近三个会计年度审计报告","研发项目立项与费用归集说明"]',
 '["提交申请","专家评审","认定公示","证书颁发"]', 45,
 '[{"key":"enterpriseName","label":"企业名称","type":"text","required":true},
   {"key":"creditCode","label":"统一社会信用代码","type":"text","required":true,"ocr":"BUSINESS_LICENSE"},
   {"key":"legalPerson","label":"法定代表人","type":"text","required":true,"ocr":"BUSINESS_LICENSE"},
   {"key":"industry","label":"所属行业","type":"text","required":true},
   {"key":"employees","label":"员工人数","type":"number","required":true},
   {"key":"rdRatio","label":"近三年研发费用占比(%)","type":"number","required":true},
   {"key":"patents","label":"有效知识产权数量","type":"number","required":true},
   {"key":"contactPhone","label":"联系人电话","type":"text","required":true}]',
 1),
(4, 'SP-BZ-001', '小规模纳税人增值税减免确认', 4, '税务局',
 '符合小规模纳税人条件的，在线确认即可享受增值税减免(免申即享)。',
 '适用对象：年应税销售额未超标准、行业符合条件的小规模纳税人。',
 '["登记为小规模纳税人","年应税销售额未超标准"]',
 '["无需提交材料(系统自动核验)"]',
 '["系统识别匹配","企业在线确认","同步税务系统","减免生效"]', 1,
 '[{"key":"enterpriseName","label":"企业名称","type":"text","required":true},
   {"key":"creditCode","label":"统一社会信用代码","type":"text","required":true,"ocr":"BUSINESS_LICENSE"},
   {"key":"annualSales","label":"上年应税销售额(万元)","type":"number","required":true},
   {"key":"employees","label":"员工人数","type":"number","required":true},
   {"key":"confirm","label":"本人已知悉并确认符合减免条件","type":"checkbox","required":true}]',
 1);

-- 演示企业信息(演示企业:东软智膳餐饮管理有限公司)
INSERT INTO enterprise_info
(id, credit_code, enterprise_name, legal_person, register_address, contact_phone, enterprise_type, establish_date, status) VALUES
(1, '91110108MA01KJX22X', '东软智膳餐饮管理有限公司', '李四',
 '北京市海淀区中关村软件园二期甲1号', '13800138000', '有限责任公司', '2022-05-18', 1),
(2, '91110108MA03BKF15Y', '云启科技有限公司', '王五',
 '北京市朝阳区望京SOHO T3', '13900139000', '有限责任公司', '2021-11-02', 1);

-- 演示电子证照(用于"免证办"自动带证 + OCR交叉核验源)
INSERT INTO enterprise_license
(enterprise_id, license_type, license_no, license_name, issue_date, expiry_date, file_url, status) VALUES
(1, '营业执照', '91110108MA01KJX22X', '东软智膳餐饮管理有限公司营业执照', '2022-05-18', '2099-12-31', NULL, 1),
(1, '食品经营许可证', 'JY11101080001123', '东软智膳餐饮管理有限公司食品经营许可证', '2023-03-01', '2028-02-29', NULL, 1),
(1, '卫生许可证', 'WS11101080004455', '东软智膳餐饮管理有限公司卫生许可证', '2025-10-20', '2026-10-20', NULL, 1),
(2, '营业执照', '91110108MA03BKF15Y', '云启科技有限公司营业执照', '2021-11-02', '2099-12-31', NULL, 1);

-- 演示政策匹配(免申即享待确认)
INSERT INTO ent_policy_match (enterprise_id, policy_title, policy_content, match_score, is_applied) VALUES
(1, '小微企业增值税减免政策(2026年度)',
 '自2026年1月1日至2026年12月31日，对月销售额10万元以下(含本数)的增值税小规模纳税人，免征增值税。',
 92.50, 0),
(1, '餐饮业稳岗返还补贴',
 '对不裁员或少裁员的参保餐饮企业，按上年度实际缴纳失业保险费的60%给予稳岗返还。',
 85.00, 0),
(2, '高新技术企业税收优惠(减按15%征收)',
 '国家需要重点扶持的高新技术企业，减按15%的税率征收企业所得税。',
 88.00, 0);

-- =============================================================
-- 10. 好差评表(办结后评价,7.5 好差评)
-- =============================================================
CREATE TABLE ent_evaluation (
    id            BIGINT       PRIMARY KEY AUTO_INCREMENT,
    record_id     BIGINT       NOT NULL COMMENT '申报记录ID',
    enterprise_id BIGINT       NOT NULL COMMENT '企业ID',
    user_id       BIGINT       NOT NULL COMMENT '评价人ID',
    star_rating   TINYINT      NOT NULL COMMENT '星级1-5',
    content       VARCHAR(500) COMMENT '文字评价',
    tags          VARCHAR(200) COMMENT '标签(JSON数组)',
    create_time   DATETIME     DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_record (record_id),
    INDEX idx_enterprise (enterprise_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='企业好差评表';

-- =============================================================
-- 11. 一件事套餐表 + 套餐事项关联表(7.1/7.2 一件事套餐·一表申报)
-- =============================================================
CREATE TABLE ent_package (
    id           BIGINT       PRIMARY KEY AUTO_INCREMENT,
    package_code VARCHAR(50)  UNIQUE,
    package_name VARCHAR(200) NOT NULL,
    description  VARCHAR(500),
    status       TINYINT      DEFAULT 1,
    create_time  DATETIME     DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='一件事套餐表';

CREATE TABLE ent_package_item (
    id         BIGINT PRIMARY KEY AUTO_INCREMENT,
    package_id BIGINT NOT NULL,
    matter_id  BIGINT NOT NULL,
    sort_order INT    DEFAULT 0,
    INDEX idx_package (package_id),
    INDEX idx_matter (matter_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='套餐事项关联表';

-- 演示套餐:开办餐饮公司一件事(企业开办 + 食品经营许可)
INSERT INTO ent_package (id, package_code, package_name, description, status) VALUES
(1, 'TC-KB-001', '开办餐饮公司一件事',
 '整合企业开办(营业执照核发)与食品经营许可,一次申报、一表申请、一套材料、并联办理。',
 1);

INSERT INTO ent_package_item (package_id, matter_id, sort_order) VALUES
(1, 1, 1),
(1, 2, 2);

-- =============================================================
-- 12. 企业智能问答记录表(历史记录)
-- =============================================================
CREATE TABLE ent_qa_record (
    id            BIGINT       PRIMARY KEY AUTO_INCREMENT,
    enterprise_id BIGINT       NOT NULL,
    user_id       BIGINT       NOT NULL,
    question      VARCHAR(500) NOT NULL,
    answer        TEXT,
    sources       TEXT         COMMENT '引用来源(JSON数组)',
    create_time   DATETIME     DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_enterprise (enterprise_id),
    INDEX idx_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='企业智能问答记录表';

-- =============================================================
-- 12-1. 企业智能问答会话表(支持历史会话切换)
-- =============================================================
CREATE TABLE ent_qa_session (
    id            BIGINT       PRIMARY KEY AUTO_INCREMENT,
    enterprise_id BIGINT       NOT NULL,
    user_id       BIGINT       NOT NULL,
    title         VARCHAR(100) NOT NULL COMMENT '会话标题(首条问题前20字)',
    message_count INT          DEFAULT 0,
    last_time     DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '最后活跃时间',
    create_time   DATETIME     DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_enterprise (enterprise_id),
    INDEX idx_last_time (enterprise_id, last_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='企业智能问答会话表';

-- 给问答记录表补充会话ID字段(已部署库升级用;新库建议直接并入建表语句)
ALTER TABLE ent_qa_record ADD COLUMN IF NOT EXISTS session_id BIGINT NULL COMMENT '所属会话ID' AFTER user_id;
ALTER TABLE ent_qa_record ADD INDEX IF NOT EXISTS idx_session (session_id);

-- 旧数据迁移:无 session_id 的记录,每条单独建一个会话,标题取问题前 20 字
INSERT INTO ent_qa_session (enterprise_id, user_id, title, message_count, last_time, create_time)
SELECT enterprise_id, user_id, LEFT(question, 20), 1, create_time, create_time
FROM ent_qa_record WHERE session_id IS NULL;
UPDATE ent_qa_record r
JOIN ent_qa_session s ON s.enterprise_id = r.enterprise_id AND s.create_time = r.create_time
SET r.session_id = s.id WHERE r.session_id IS NULL;
