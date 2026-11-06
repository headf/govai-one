-- =============================================================
-- 智能问答 · 会话功能升级脚本
-- 功能:支持「点击历史记录返回历史对话窗口」
-- 执行方式:在 IDEA Database 面板连接 govai_enterprise 库后执行本脚本
-- 说明:可重复执行(IF NOT EXISTS 保护);旧数据自动迁移
-- =============================================================

-- 1. 新建会话表
CREATE TABLE IF NOT EXISTS ent_qa_session (
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

-- 2. 给问答记录表补充会话ID字段(MySQL 8.4 不支持 IF NOT EXISTS 子句,用 --force 忽略重复执行错误)
ALTER TABLE ent_qa_record ADD COLUMN session_id BIGINT NULL COMMENT '所属会话ID' AFTER user_id;
ALTER TABLE ent_qa_record ADD INDEX idx_session (session_id);

-- 3. 旧数据迁移:无 session_id 的记录,每条单独建一个会话,标题取问题前 20 字
INSERT INTO ent_qa_session (enterprise_id, user_id, title, message_count, last_time, create_time)
SELECT enterprise_id, user_id, LEFT(question, 20), 1, create_time, create_time
FROM ent_qa_record
WHERE session_id IS NULL;

UPDATE ent_qa_record r
JOIN ent_qa_session s
  ON s.enterprise_id = r.enterprise_id
 AND s.create_time = r.create_time
SET r.session_id = s.id
WHERE r.session_id IS NULL;

-- 4. 验证
SELECT 'ent_qa_session 表:' AS info, COUNT(*) AS rows_count FROM ent_qa_session;
SELECT 'ent_qa_record.session_id 已回填:' AS info, COUNT(*) AS filled FROM ent_qa_record WHERE session_id IS NOT NULL;
