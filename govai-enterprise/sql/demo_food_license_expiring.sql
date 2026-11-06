-- 演示:把食品经营许可证到期日改近(30天内),触发证照页到期提醒 + "去办理延期"按钮
-- 食品经营许可证有对应延续事项(id=6 SP-JY-003 食品经营许可延续)
-- 执行后:企业证照页顶部到期提醒将显示食品经营许可证到期 + [去办理延期]按钮 → 跳转 /matters/6/apply
UPDATE enterprise_license
SET expiry_date = '2026-10-01'
WHERE license_type LIKE '%食品经营许可%';

-- 验证
SELECT id, license_type, license_name, expiry_date,
       DATEDIFF(expiry_date, CURDATE()) AS days_to_expire
FROM enterprise_license
WHERE license_type LIKE '%食品经营许可%';
