-- ============================================================
-- 差评整改工单表（好差评闭环）
-- 背景：群众端 / 企业端提交好差评时，若为差评(星级 <= 2)，
--       通过 Feign 上报监管端，生成一条待处置工单；
--       监管端在"差评整改"页面处置，形成"差评 → 整改 → 反馈"闭环。
-- 对接：群众端 POST /api/v1/review-orders/report
--       企业端 POST /api/v1/review-orders/report
-- ============================================================
CREATE TABLE IF NOT EXISTS review_order (
    id              BIGINT       PRIMARY KEY AUTO_INCREMENT,
    review_no       VARCHAR(40)  COMMENT '工单编号(XP+日期+4位随机)',
    source          VARCHAR(20)  COMMENT '来源: CITIZEN 群众端 / ENTERPRISE 企业端',
    applicant_name  VARCHAR(50)  COMMENT '评价人(群众姓名 / 企业经办人)',
    matter_name     VARCHAR(100) COMMENT '申报事项名称',
    record_code     VARCHAR(50)  COMMENT '申报编号(两端业务码)',
    star_rating     INT          COMMENT '星级 1-5(1-2 视为差评)',
    content         VARCHAR(500) COMMENT '评价内容',
    tags            VARCHAR(200) COMMENT '评价标签(逗号分隔)',
    handle_status   VARCHAR(20)  DEFAULT 'PENDING' COMMENT 'PENDING 待处置 / HANDLED 已处置',
    handle_opinion  VARCHAR(500) COMMENT '整改处置意见',
    handler_name    VARCHAR(50)  COMMENT '处置人',
    create_time     DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '上报时间',
    handle_time     DATETIME     COMMENT '处置时间',
    INDEX idx_handle_status (handle_status),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='差评整改工单';
