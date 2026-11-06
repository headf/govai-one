-- 智能办事规划历史表(记忆功能)
CREATE TABLE IF NOT EXISTS `ent_matter_plan_history` (
  `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `enterprise_id` BIGINT       NOT NULL                COMMENT '企业ID',
  `user_id`       BIGINT       NOT NULL                COMMENT '用户ID',
  `description`   VARCHAR(500) NOT NULL               COMMENT '用户业务需求描述',
  `has_license`   TINYINT      DEFAULT 0               COMMENT '是否已有营业执照 0否1是',
  `title`         VARCHAR(200)                         COMMENT '规划标题',
  `summary`       VARCHAR(1000)                        COMMENT '规划概述',
  `plan_json`     TEXT                                 COMMENT '规划结果JSON(items/tips)',
  `create_time`   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '规划时间',
  PRIMARY KEY (`id`),
  KEY `idx_ent_plan_enterprise` (`enterprise_id`),
  KEY `idx_ent_plan_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='智能办事规划历史';

-- 插入1条示例历史(东软智膳企业,用户ent_admin)
INSERT INTO `ent_matter_plan_history` (`enterprise_id`, `user_id`, `description`, `has_license`, `title`, `summary`, `plan_json`)
VALUES (1, 1, '我想开一家食品销售便利店,卖点零食饮料和烟草', 1,
        '「食品销售便利店」办理清单',
        '已有营业执照,主要需办理食品经营许可证和烟草专卖零售许可证,建议先办食品许可再办烟草许可。',
        '{"items":[{"matterId":2,"matterName":"食品经营许可新办","order":1,"reason":"销售食品必须取得食品经营许可证","prerequisite":true,"status":"RECOMMENDED","deadlineDays":20,"department":"市场监督管理局"},{"matterId":14,"matterName":"烟草专卖零售许可新办","order":2,"reason":"销售烟草需取得烟草专卖零售许可证","prerequisite":false,"status":"RECOMMENDED","deadlineDays":15,"department":"烟草专卖局"}],"tips":["食品经营许可证和烟草许可证可并行申请","许可证需悬挂在经营场所醒目位置"]}')
ON DUPLICATE KEY UPDATE `id`=`id`;

SELECT id, enterprise_id, description, title, create_time FROM ent_matter_plan_history ORDER BY create_time DESC LIMIT 5;
