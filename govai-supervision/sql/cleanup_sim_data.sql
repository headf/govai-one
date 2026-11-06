-- =============================================================
-- GovAI 监管端：一次性清理"办件派生/模拟"数据（真实流程化重置）
-- 使用前请先备份：mysqldump 上述被清表（见实施说明）。
-- 清空范围=镜像/派生/样例表，100% 可由真实流程重建：
--   approval_data_cache  ← 企业端申报 HTTP 上报 + 审批端 RabbitMQ 推送
--   risk_alert          ← RiskScanTask 定时(5min)扫描真实缓存重建
--   audit_trace         ← 监管员真实操作(如后续自动留痕)
--   supervision_sample  ← 监管员真实发起抽查重建
--   stats_daily         ← 大屏时序已改纯实时聚合，样例表不再使用
-- 保留：sys_*（RBAC/用户/字典/操作日志）、credit_enterprise/personal/repair（信用档案）、risk_rule（预警规则）
-- 如需约束权限残留，请另跑 sql/upgrade.sql（幂等删除 approval:push/delete）
-- =============================================================

DELETE FROM approval_data_cache;
DELETE FROM risk_alert;
DELETE FROM audit_trace;
DELETE FROM supervision_sample;
DELETE FROM stats_daily;
