-- ============================================================
-- GovAI 审批端: 全事项规则集种子(按企业端 18 个事项编码补全)
-- 库：govai_approval
-- 执行：mysql -uroot -p123456 --default-character-set=utf8mb4 govai_approval < rule_set_seed_all_matters.sql
-- 幂等：按 matter_type(事项编码)存在即跳过,可重复执行,不覆盖/不改写已有规则集与规则项
--
-- 为什么补全:
--   schema.sql 演示规则集挂在 ENTERPRISE_REGISTER(旧模板编码),当前流程模板已改为按企业端
--   matterCode(SP-JY-001…)作 template_code+matter_type,故旧规则集永不命中。
--   本脚本为 18 个真实申报事项各配一套规则集,使申报提交到审批端时规则引擎真实执行:
--     · 规则全过   -> 即办件·系统自动办结(AUTO 秒批),回调企业端出证
--     · 规则不过   -> 转人工最终判定(创建首环节任务)
--
-- 引擎能力约束(RuleEngineService 为准):
--   NUMBER 操作符 GT/LT/EQ/BETWEEN(compare_value "min,max")
--   TEXT   操作符 EQ/CONTAINS
--   DATE   操作符 GT/LT/EQ/BETWEEN(compare_value 可用 ${today})
--   LICENSE 该字段非空即通过(实训简化:有证即过,忽略操作符/比较值)
--   同一规则集 items 按 rule_set.logic_type 统一 AND(全过)/OR(任一过);
--   item.logic_operator 引擎不消费,此处统一写 AND。
--
-- 设计约束(避免"不可控转人工"): 规则字段取自各事项 form_config 真实 key,
--   且只落在可稳定预判的类型上——
--   · NUMBER/DATE 业务阈值:填合法值即过、填越界值即转人工(演示可控)
--   · LICENSE 证照号/统一社会信用代码:有证即过(演示主链路稳定)
--   · TEXT 仅对"选项值固定"的 select/checkbox 用 EQ/CONTAINS(如 changeType 选项均含"变更")
--   ⚠️ 不使用自由文本关键词(地址/姓名/说明类),避免申报演示时意外转人工。
--   每事项至少 1 条"可触发转人工"的差异化规则,便于演示两种结果。
--
-- 效果提示: 规则全过的申报不再进人工审批而是即办秒批;要演示人工审批,
--   让某条规则判不过即可(如把研发占比填低、面积填小、比例越界、注销选"未清算"等)。
-- ============================================================

-- ------------------------------------------------------------
-- 1. 企业开办一件事(QY-KB-001)
--    触发点: 注册资本≤1万转人工
-- ------------------------------------------------------------
INSERT INTO approval_rule_set (rule_set_name, matter_type, status, logic_type, description)
SELECT '企业开办合规审查', 'QY-KB-001', 1, 'AND', '企业开办一件事自动审查:注册资本认缴额+营业执照核验'
WHERE NOT EXISTS (SELECT 1 FROM approval_rule_set WHERE matter_type = 'QY-KB-001');

INSERT INTO approval_rule_item (rule_set_id, rule_type, field_name, operator, compare_value, logic_operator, sort_order, error_message)
SELECT s.id, 'NUMBER', 'registerCapital', 'GT', '1', 'AND', 1, '注册资本应大于1万元(认缴制最低额)'
FROM approval_rule_set s WHERE s.matter_type = 'QY-KB-001'
  AND NOT EXISTS (SELECT 1 FROM approval_rule_item i WHERE i.rule_set_id = s.id AND i.field_name = 'registerCapital');

INSERT INTO approval_rule_item (rule_set_id, rule_type, field_name, operator, compare_value, logic_operator, sort_order, error_message)
SELECT s.id, 'LICENSE', 'creditCode', 'EQ', 'true', 'AND', 2, '营业执照(统一社会信用代码)未自动核验通过'
FROM approval_rule_set s WHERE s.matter_type = 'QY-KB-001'
  AND NOT EXISTS (SELECT 1 FROM approval_rule_item i WHERE i.rule_set_id = s.id AND i.field_name = 'creditCode');

-- ------------------------------------------------------------
-- 2. 食品经营许可新办(SP-JY-001)
--    字段: operateArea(㎡/number) foodCategory(select 选项均含"食品")
--    触发点: 面积≤5 或经营项目异常 转人工
-- ------------------------------------------------------------
INSERT INTO approval_rule_set (rule_set_name, matter_type, status, logic_type, description)
SELECT '食品经营许可新办审查', 'SP-JY-001', 1, 'AND', '食品经营许可新办:场所面积规模+经营项目+证照自动审查'
WHERE NOT EXISTS (SELECT 1 FROM approval_rule_set WHERE matter_type = 'SP-JY-001');

INSERT INTO approval_rule_item (rule_set_id, rule_type, field_name, operator, compare_value, logic_operator, sort_order, error_message)
SELECT s.id, 'NUMBER', 'operateArea', 'GT', '5', 'AND', 1, '经营场所面积过小(<5㎡),建议人工现场核查'
FROM approval_rule_set s WHERE s.matter_type = 'SP-JY-001'
  AND NOT EXISTS (SELECT 1 FROM approval_rule_item i WHERE i.rule_set_id = s.id AND i.field_name = 'operateArea');

INSERT INTO approval_rule_item (rule_set_id, rule_type, field_name, operator, compare_value, logic_operator, sort_order, error_message)
SELECT s.id, 'TEXT', 'foodCategory', 'CONTAINS', '食品', 'AND', 2, '经营项目应为热食/冷食/预包装/散装/糕点类等食品经营项目'
FROM approval_rule_set s WHERE s.matter_type = 'SP-JY-001'
  AND NOT EXISTS (SELECT 1 FROM approval_rule_item i WHERE i.rule_set_id = s.id AND i.field_name = 'foodCategory');

INSERT INTO approval_rule_item (rule_set_id, rule_type, field_name, operator, compare_value, logic_operator, sort_order, error_message)
SELECT s.id, 'LICENSE', 'creditCode', 'EQ', 'true', 'AND', 3, '营业执照未自动核验通过'
FROM approval_rule_set s WHERE s.matter_type = 'SP-JY-001'
  AND NOT EXISTS (SELECT 1 FROM approval_rule_item i WHERE i.rule_set_id = s.id AND i.field_name = 'creditCode');

-- ------------------------------------------------------------
-- 3. 高新技术企业认定(ZZ-GX-001)
--    字段: rdRatio(研发占比%/number) patents(知产数/number) employees(人数/number)
--    触发点: 任一硬门槛不过转人工(研发占比填低即可演示转人工)
-- ------------------------------------------------------------
INSERT INTO approval_rule_set (rule_set_name, matter_type, status, logic_type, description)
SELECT '高新技术企业认定审查', 'ZZ-GX-001', 1, 'AND', '高企认定自动初审:研发占比/知产/员工规模硬门槛'
WHERE NOT EXISTS (SELECT 1 FROM approval_rule_set WHERE matter_type = 'ZZ-GX-001');

INSERT INTO approval_rule_item (rule_set_id, rule_type, field_name, operator, compare_value, logic_operator, sort_order, error_message)
SELECT s.id, 'NUMBER', 'rdRatio', 'GT', '5', 'AND', 1, '近三年研发费用占比应不低于5%(认定基本门槛)'
FROM approval_rule_set s WHERE s.matter_type = 'ZZ-GX-001'
  AND NOT EXISTS (SELECT 1 FROM approval_rule_item i WHERE i.rule_set_id = s.id AND i.field_name = 'rdRatio');

INSERT INTO approval_rule_item (rule_set_id, rule_type, field_name, operator, compare_value, logic_operator, sort_order, error_message)
SELECT s.id, 'NUMBER', 'patents', 'GT', '0', 'AND', 2, '应拥有至少1项有效知识产权(发明专利/实用新型/软著等)'
FROM approval_rule_set s WHERE s.matter_type = 'ZZ-GX-001'
  AND NOT EXISTS (SELECT 1 FROM approval_rule_item i WHERE i.rule_set_id = s.id AND i.field_name = 'patents');

INSERT INTO approval_rule_item (rule_set_id, rule_type, field_name, operator, compare_value, logic_operator, sort_order, error_message)
SELECT s.id, 'NUMBER', 'employees', 'GT', '10', 'AND', 3, '员工人数建议不少于10人(科技人员占比前置核验)'
FROM approval_rule_set s WHERE s.matter_type = 'ZZ-GX-001'
  AND NOT EXISTS (SELECT 1 FROM approval_rule_item i WHERE i.rule_set_id = s.id AND i.field_name = 'employees');

-- ------------------------------------------------------------
-- 4. 小规模纳税人增值税减免确认(SP-BZ-001) —— 免申即享
--    字段: annualSales(上年应税销售额万元/number) confirm(勾选/checkbox=true)
--    触发点: 销售额超 120 万或未勾选 转人工
-- ------------------------------------------------------------
INSERT INTO approval_rule_set (rule_set_name, matter_type, status, logic_type, description)
SELECT '增值税减免资格核验', 'SP-BZ-001', 1, 'AND', '小规模纳税人增值税减免:年应税销售额上限+确认声明核验'
WHERE NOT EXISTS (SELECT 1 FROM approval_rule_set WHERE matter_type = 'SP-BZ-001');

INSERT INTO approval_rule_item (rule_set_id, rule_type, field_name, operator, compare_value, logic_operator, sort_order, error_message)
SELECT s.id, 'NUMBER', 'annualSales', 'LT', '120', 'AND', 1, '上年应税销售额应低于120万元(月销售额10万免征标准)'
FROM approval_rule_set s WHERE s.matter_type = 'SP-BZ-001'
  AND NOT EXISTS (SELECT 1 FROM approval_rule_item i WHERE i.rule_set_id = s.id AND i.field_name = 'annualSales');

INSERT INTO approval_rule_item (rule_set_id, rule_type, field_name, operator, compare_value, logic_operator, sort_order, error_message)
SELECT s.id, 'TEXT', 'confirm', 'EQ', 'true', 'AND', 2, '须勾选"已知悉并确认符合减免条件"'
FROM approval_rule_set s WHERE s.matter_type = 'SP-BZ-001'
  AND NOT EXISTS (SELECT 1 FROM approval_rule_item i WHERE i.rule_set_id = s.id AND i.field_name = 'confirm');

-- ------------------------------------------------------------
-- 5. 食品经营许可变更(SP-JY-002)
--    字段: licenseNo(原证/LICENSE) changeType(选项均含"变更")
-- ------------------------------------------------------------
INSERT INTO approval_rule_set (rule_set_name, matter_type, status, logic_type, description)
SELECT '食品经营许可变更审查', 'SP-JY-002', 1, 'AND', '食品经营许可变更:原证在库核验+变更事项有效'
WHERE NOT EXISTS (SELECT 1 FROM approval_rule_set WHERE matter_type = 'SP-JY-002');

INSERT INTO approval_rule_item (rule_set_id, rule_type, field_name, operator, compare_value, logic_operator, sort_order, error_message)
SELECT s.id, 'LICENSE', 'licenseNo', 'EQ', 'true', 'AND', 1, '原食品经营许可证未核验通过或已失效'
FROM approval_rule_set s WHERE s.matter_type = 'SP-JY-002'
  AND NOT EXISTS (SELECT 1 FROM approval_rule_item i WHERE i.rule_set_id = s.id AND i.field_name = 'licenseNo');

INSERT INTO approval_rule_item (rule_set_id, rule_type, field_name, operator, compare_value, logic_operator, sort_order, error_message)
SELECT s.id, 'TEXT', 'changeType', 'CONTAINS', '变更', 'AND', 2, '变更事项类型无效'
FROM approval_rule_set s WHERE s.matter_type = 'SP-JY-002'
  AND NOT EXISTS (SELECT 1 FROM approval_rule_item i WHERE i.rule_set_id = s.id AND i.field_name = 'changeType');

-- ------------------------------------------------------------
-- 6. 食品经营许可延续(SP-JY-003)
--    字段: licenseNo  originalExpiryDate(原证到期日/date)
--    触发点: 原证已过期仍申延续 转人工
-- ------------------------------------------------------------
INSERT INTO approval_rule_set (rule_set_name, matter_type, status, logic_type, description)
SELECT '食品经营许可延续审查', 'SP-JY-003', 1, 'AND', '食品经营许可延续:原证在库+未过期(到期前可续)'
WHERE NOT EXISTS (SELECT 1 FROM approval_rule_set WHERE matter_type = 'SP-JY-003');

INSERT INTO approval_rule_item (rule_set_id, rule_type, field_name, operator, compare_value, logic_operator, sort_order, error_message)
SELECT s.id, 'LICENSE', 'licenseNo', 'EQ', 'true', 'AND', 1, '原食品经营许可证未核验通过或已注销'
FROM approval_rule_set s WHERE s.matter_type = 'SP-JY-003'
  AND NOT EXISTS (SELECT 1 FROM approval_rule_item i WHERE i.rule_set_id = s.id AND i.field_name = 'licenseNo');

INSERT INTO approval_rule_item (rule_set_id, rule_type, field_name, operator, compare_value, logic_operator, sort_order, error_message)
SELECT s.id, 'DATE', 'originalExpiryDate', 'GT', '${today}', 'AND', 2, '原许可证已过期,应办理新办而非延续'
FROM approval_rule_set s WHERE s.matter_type = 'SP-JY-003'
  AND NOT EXISTS (SELECT 1 FROM approval_rule_item i WHERE i.rule_set_id = s.id AND i.field_name = 'originalExpiryDate');

-- ------------------------------------------------------------
-- 7. 食品经营许可注销(SP-JY-004)
--    字段: licenseNo  cancelReason(选项含"终止经营")
--    触发点: 注销原因非"终止经营"(转行/其他)转人工复核
-- ------------------------------------------------------------
INSERT INTO approval_rule_set (rule_set_name, matter_type, status, logic_type, description)
SELECT '食品经营许可注销审查', 'SP-JY-004', 1, 'AND', '食品经营许可注销:原证交回核验+注销原因复核'
WHERE NOT EXISTS (SELECT 1 FROM approval_rule_set WHERE matter_type = 'SP-JY-004');

INSERT INTO approval_rule_item (rule_set_id, rule_type, field_name, operator, compare_value, logic_operator, sort_order, error_message)
SELECT s.id, 'LICENSE', 'licenseNo', 'EQ', 'true', 'AND', 1, '原食品经营许可证未核验通过'
FROM approval_rule_set s WHERE s.matter_type = 'SP-JY-004'
  AND NOT EXISTS (SELECT 1 FROM approval_rule_item i WHERE i.rule_set_id = s.id AND i.field_name = 'licenseNo');

INSERT INTO approval_rule_item (rule_set_id, rule_type, field_name, operator, compare_value, logic_operator, sort_order, error_message)
SELECT s.id, 'TEXT', 'cancelReason', 'CONTAINS', '经营', 'AND', 2, '终止经营类注销请选择"终止经营";转行/其他原因转人工复核'
FROM approval_rule_set s WHERE s.matter_type = 'SP-JY-004'
  AND NOT EXISTS (SELECT 1 FROM approval_rule_item i WHERE i.rule_set_id = s.id AND i.field_name = 'cancelReason');

-- ------------------------------------------------------------
-- 8. 企业变更一件事(QY-KB-002)
--    字段: changeType(选项均含"变更") creditCode
-- ------------------------------------------------------------
INSERT INTO approval_rule_set (rule_set_name, matter_type, status, logic_type, description)
SELECT '企业变更一件事审查', 'QY-KB-002', 1, 'AND', '企业变更联办自动审查:变更事项有效+营业执照在库'
WHERE NOT EXISTS (SELECT 1 FROM approval_rule_set WHERE matter_type = 'QY-KB-002');

INSERT INTO approval_rule_item (rule_set_id, rule_type, field_name, operator, compare_value, logic_operator, sort_order, error_message)
SELECT s.id, 'TEXT', 'changeType', 'CONTAINS', '变更', 'AND', 1, '变更事项类型无效'
FROM approval_rule_set s WHERE s.matter_type = 'QY-KB-002'
  AND NOT EXISTS (SELECT 1 FROM approval_rule_item i WHERE i.rule_set_id = s.id AND i.field_name = 'changeType');

INSERT INTO approval_rule_item (rule_set_id, rule_type, field_name, operator, compare_value, logic_operator, sort_order, error_message)
SELECT s.id, 'LICENSE', 'creditCode', 'EQ', 'true', 'AND', 2, '营业执照未自动核验通过'
FROM approval_rule_set s WHERE s.matter_type = 'QY-KB-002'
  AND NOT EXISTS (SELECT 1 FROM approval_rule_item i WHERE i.rule_set_id = s.id AND i.field_name = 'creditCode');

-- ------------------------------------------------------------
-- 9. 企业简易注销一件事(QY-KB-003)
--    字段: liquidationComplete(select) noTaxIssue(select) cancelDetail(说明/textarea)
--    触发点: 未清算完结 / 存在税务未办结 任一 -> 转人工(强门槛)
-- ------------------------------------------------------------
INSERT INTO approval_rule_set (rule_set_name, matter_type, status, logic_type, description)
SELECT '企业简易注销审查', 'QY-KB-003', 1, 'AND', '企业简易注销三门槛:清算完结+无税务未办结+说明完整'
WHERE NOT EXISTS (SELECT 1 FROM approval_rule_set WHERE matter_type = 'QY-KB-003');

INSERT INTO approval_rule_item (rule_set_id, rule_type, field_name, operator, compare_value, logic_operator, sort_order, error_message)
SELECT s.id, 'TEXT', 'liquidationComplete', 'EQ', '已清算完结', 'AND', 1, '清算未完结企业不可简易注销'
FROM approval_rule_set s WHERE s.matter_type = 'QY-KB-003'
  AND NOT EXISTS (SELECT 1 FROM approval_rule_item i WHERE i.rule_set_id = s.id AND i.field_name = 'liquidationComplete');

INSERT INTO approval_rule_item (rule_set_id, rule_type, field_name, operator, compare_value, logic_operator, sort_order, error_message)
SELECT s.id, 'TEXT', 'noTaxIssue', 'EQ', '是', 'AND', 2, '存在税务未办结事项,不可简易注销'
FROM approval_rule_set s WHERE s.matter_type = 'QY-KB-003'
  AND NOT EXISTS (SELECT 1 FROM approval_rule_item i WHERE i.rule_set_id = s.id AND i.field_name = 'noTaxIssue');

INSERT INTO approval_rule_item (rule_set_id, rule_type, field_name, operator, compare_value, logic_operator, sort_order, error_message)
SELECT s.id, 'TEXT', 'cancelDetail', 'CONTAINS', '清算', 'AND', 3, '注销情况说明请填写清算情况'
FROM approval_rule_set s WHERE s.matter_type = 'QY-KB-003'
  AND NOT EXISTS (SELECT 1 FROM approval_rule_item i WHERE i.rule_set_id = s.id AND i.field_name = 'cancelDetail');

-- ------------------------------------------------------------
-- 10. 餐饮业稳岗返还补贴申报(SP-BZ-002)
--    字段: employeeCount(number) lastYearInsuranceAmount(元/number) bankName(开户行名)
--    触发点: 用工人数/缴费金额异常 转人工
-- ------------------------------------------------------------
INSERT INTO approval_rule_set (rule_set_name, matter_type, status, logic_type, description)
SELECT '餐饮稳岗返还补贴审查', 'SP-BZ-002', 1, 'AND', '餐饮业稳岗返还:用工人数/缴费基数/收款账户自动初审'
WHERE NOT EXISTS (SELECT 1 FROM approval_rule_set WHERE matter_type = 'SP-BZ-002');

INSERT INTO approval_rule_item (rule_set_id, rule_type, field_name, operator, compare_value, logic_operator, sort_order, error_message)
SELECT s.id, 'NUMBER', 'employeeCount', 'GT', '0', 'AND', 1, '用工人数应大于0'
FROM approval_rule_set s WHERE s.matter_type = 'SP-BZ-002'
  AND NOT EXISTS (SELECT 1 FROM approval_rule_item i WHERE i.rule_set_id = s.id AND i.field_name = 'employeeCount');

INSERT INTO approval_rule_item (rule_set_id, rule_type, field_name, operator, compare_value, logic_operator, sort_order, error_message)
SELECT s.id, 'NUMBER', 'lastYearInsuranceAmount', 'GT', '0', 'AND', 2, '上年度失业保险缴费金额应大于0'
FROM approval_rule_set s WHERE s.matter_type = 'SP-BZ-002'
  AND NOT EXISTS (SELECT 1 FROM approval_rule_item i WHERE i.rule_set_id = s.id AND i.field_name = 'lastYearInsuranceAmount');

INSERT INTO approval_rule_item (rule_set_id, rule_type, field_name, operator, compare_value, logic_operator, sort_order, error_message)
SELECT s.id, 'TEXT', 'bankName', 'CONTAINS', '银行', 'AND', 3, '开户银行名称无效(应填写如 中国工商银行)'
FROM approval_rule_set s WHERE s.matter_type = 'SP-BZ-002'
  AND NOT EXISTS (SELECT 1 FROM approval_rule_item i WHERE i.rule_set_id = s.id AND i.field_name = 'bankName');

-- ------------------------------------------------------------
-- 11. 公共场所卫生许可新办(WS-WS-001)
--    字段: operateArea(number) placeType(select 选项为营业场所,含"馆/店/浴"类) creditCode
--    触发点: 面积过小 / 场所类型不典型 转人工
-- ------------------------------------------------------------
INSERT INTO approval_rule_set (rule_set_name, matter_type, status, logic_type, description)
SELECT '公共场所卫生许可新办审查', 'WS-WS-001', 1, 'AND', '公共场所卫生许可:场所面积+场所类型+证照自动初审'
WHERE NOT EXISTS (SELECT 1 FROM approval_rule_set WHERE matter_type = 'WS-WS-001');

INSERT INTO approval_rule_item (rule_set_id, rule_type, field_name, operator, compare_value, logic_operator, sort_order, error_message)
SELECT s.id, 'NUMBER', 'operateArea', 'GT', '10', 'AND', 1, '公共场所经营面积过小(<10㎡),建议现场核查'
FROM approval_rule_set s WHERE s.matter_type = 'WS-WS-001'
  AND NOT EXISTS (SELECT 1 FROM approval_rule_item i WHERE i.rule_set_id = s.id AND i.field_name = 'operateArea');

INSERT INTO approval_rule_item (rule_set_id, rule_type, field_name, operator, compare_value, logic_operator, sort_order, error_message)
SELECT s.id, 'LICENSE', 'creditCode', 'EQ', 'true', 'AND', 2, '营业执照未自动核验通过'
FROM approval_rule_set s WHERE s.matter_type = 'WS-WS-001'
  AND NOT EXISTS (SELECT 1 FROM approval_rule_item i WHERE i.rule_set_id = s.id AND i.field_name = 'creditCode');

-- ------------------------------------------------------------
-- 12. 公共场所卫生许可延续(WS-WS-002)
--    字段: licenseNo originalExpiryDate
-- ------------------------------------------------------------
INSERT INTO approval_rule_set (rule_set_name, matter_type, status, logic_type, description)
SELECT '公共场所卫生许可延续审查', 'WS-WS-002', 1, 'AND', '公共场所卫生许可延续:原证在库+未过期'
WHERE NOT EXISTS (SELECT 1 FROM approval_rule_set WHERE matter_type = 'WS-WS-002');

INSERT INTO approval_rule_item (rule_set_id, rule_type, field_name, operator, compare_value, logic_operator, sort_order, error_message)
SELECT s.id, 'LICENSE', 'licenseNo', 'EQ', 'true', 'AND', 1, '原卫生许可证未核验通过或已注销'
FROM approval_rule_set s WHERE s.matter_type = 'WS-WS-002'
  AND NOT EXISTS (SELECT 1 FROM approval_rule_item i WHERE i.rule_set_id = s.id AND i.field_name = 'licenseNo');

INSERT INTO approval_rule_item (rule_set_id, rule_type, field_name, operator, compare_value, logic_operator, sort_order, error_message)
SELECT s.id, 'DATE', 'originalExpiryDate', 'GT', '${today}', 'AND', 2, '原许可证已过期,应办理新办而非延续'
FROM approval_rule_set s WHERE s.matter_type = 'WS-WS-002'
  AND NOT EXISTS (SELECT 1 FROM approval_rule_item i WHERE i.rule_set_id = s.id AND i.field_name = 'originalExpiryDate');

-- ------------------------------------------------------------
-- 13. 食品经营许可补办(SP-JY-005)
--    字段: licenseNo  lossReason(遗失/损毁) lossDetail
-- ------------------------------------------------------------
INSERT INTO approval_rule_set (rule_set_name, matter_type, status, logic_type, description)
SELECT '食品经营许可补办审查', 'SP-JY-005', 1, 'AND', '食品经营许可补办:原证在库核验+补办情形说明'
WHERE NOT EXISTS (SELECT 1 FROM approval_rule_set WHERE matter_type = 'SP-JY-005');

INSERT INTO approval_rule_item (rule_set_id, rule_type, field_name, operator, compare_value, logic_operator, sort_order, error_message)
SELECT s.id, 'LICENSE', 'licenseNo', 'EQ', 'true', 'AND', 1, '原食品经营许可证未核验通过'
FROM approval_rule_set s WHERE s.matter_type = 'SP-JY-005'
  AND NOT EXISTS (SELECT 1 FROM approval_rule_item i WHERE i.rule_set_id = s.id AND i.field_name = 'licenseNo');

INSERT INTO approval_rule_item (rule_set_id, rule_type, field_name, operator, compare_value, logic_operator, sort_order, error_message)
SELECT s.id, 'TEXT', 'lossDetail', 'CONTAINS', '证', 'AND', 2, '请填写遗失/损毁情况说明(如"许可证已遗失")'
FROM approval_rule_set s WHERE s.matter_type = 'SP-JY-005'
  AND NOT EXISTS (SELECT 1 FROM approval_rule_item i WHERE i.rule_set_id = s.id AND i.field_name = 'lossDetail');

-- ------------------------------------------------------------
-- 14. 烟草专卖零售许可新办(YC-ZM-001)
--    字段: businessScope(经营范围) creditCode
--    触发点: 经营范围不含烟草 转人工
-- ------------------------------------------------------------
INSERT INTO approval_rule_set (rule_set_name, matter_type, status, logic_type, description)
SELECT '烟草专卖零售许可审查', 'YC-ZM-001', 1, 'AND', '烟草专卖零售许可:经营范围须含烟草零售+营业执照核验'
WHERE NOT EXISTS (SELECT 1 FROM approval_rule_set WHERE matter_type = 'YC-ZM-001');

INSERT INTO approval_rule_item (rule_set_id, rule_type, field_name, operator, compare_value, logic_operator, sort_order, error_message)
SELECT s.id, 'TEXT', 'businessScope', 'CONTAINS', '烟草', 'AND', 1, '经营范围应包含烟草制品零售'
FROM approval_rule_set s WHERE s.matter_type = 'YC-ZM-001'
  AND NOT EXISTS (SELECT 1 FROM approval_rule_item i WHERE i.rule_set_id = s.id AND i.field_name = 'businessScope');

INSERT INTO approval_rule_item (rule_set_id, rule_type, field_name, operator, compare_value, logic_operator, sort_order, error_message)
SELECT s.id, 'LICENSE', 'creditCode', 'EQ', 'true', 'AND', 2, '营业执照未自动核验通过'
FROM approval_rule_set s WHERE s.matter_type = 'YC-ZM-001'
  AND NOT EXISTS (SELECT 1 FROM approval_rule_item i WHERE i.rule_set_id = s.id AND i.field_name = 'creditCode');

-- ------------------------------------------------------------
-- 15. 药品经营许可新办(YP-JY-001)
--    字段: pharmacistCertNo(药师证/LICENSE) creditCode
--    说明: 药师姓名自由文本不可靠,不设关键词;凭药师资格证号+营业执照双证核验
-- ------------------------------------------------------------
INSERT INTO approval_rule_set (rule_set_name, matter_type, status, logic_type, description)
SELECT '药品经营许可审查', 'YP-JY-001', 1, 'AND', '药品经营许可:执业药师资格证+营业执照双证核验'
WHERE NOT EXISTS (SELECT 1 FROM approval_rule_set WHERE matter_type = 'YP-JY-001');

INSERT INTO approval_rule_item (rule_set_id, rule_type, field_name, operator, compare_value, logic_operator, sort_order, error_message)
SELECT s.id, 'LICENSE', 'pharmacistCertNo', 'EQ', 'true', 'AND', 1, '执业药师资格证号未核验通过,药品经营须配备执业药师'
FROM approval_rule_set s WHERE s.matter_type = 'YP-JY-001'
  AND NOT EXISTS (SELECT 1 FROM approval_rule_item i WHERE i.rule_set_id = s.id AND i.field_name = 'pharmacistCertNo');

INSERT INTO approval_rule_item (rule_set_id, rule_type, field_name, operator, compare_value, logic_operator, sort_order, error_message)
SELECT s.id, 'LICENSE', 'creditCode', 'EQ', 'true', 'AND', 2, '营业执照未自动核验通过'
FROM approval_rule_set s WHERE s.matter_type = 'YP-JY-001'
  AND NOT EXISTS (SELECT 1 FROM approval_rule_item i WHERE i.rule_set_id = s.id AND i.field_name = 'creditCode');

-- ------------------------------------------------------------
-- 16. 排污许可新办(PW-PX-001)
--    字段: pollutantType(select) creditCode
--    触发点: 主要污染物类型不在登记范围 转人工技术审查
--    (选项: 废水/废气/噪音/固废;CONTAINS '废' 覆盖 废水/废气/固废,噪音类转人工复核)
-- ------------------------------------------------------------
INSERT INTO approval_rule_set (rule_set_name, matter_type, status, logic_type, description)
SELECT '排污许可审查', 'PW-PX-001', 1, 'AND', '排污许可:主要污染物类型核验+营业执照核验'
WHERE NOT EXISTS (SELECT 1 FROM approval_rule_set WHERE matter_type = 'PW-PX-001');

INSERT INTO approval_rule_item (rule_set_id, rule_type, field_name, operator, compare_value, logic_operator, sort_order, error_message)
SELECT s.id, 'TEXT', 'pollutantType', 'CONTAINS', '废', 'AND', 1, '主要污染物类型无效(废水/废气/固废等;噪音污染建议人工技术审查)'
FROM approval_rule_set s WHERE s.matter_type = 'PW-PX-001'
  AND NOT EXISTS (SELECT 1 FROM approval_rule_item i WHERE i.rule_set_id = s.id AND i.field_name = 'pollutantType');

INSERT INTO approval_rule_item (rule_set_id, rule_type, field_name, operator, compare_value, logic_operator, sort_order, error_message)
SELECT s.id, 'LICENSE', 'creditCode', 'EQ', 'true', 'AND', 2, '营业执照未自动核验通过'
FROM approval_rule_set s WHERE s.matter_type = 'PW-PX-001'
  AND NOT EXISTS (SELECT 1 FROM approval_rule_item i WHERE i.rule_set_id = s.id AND i.field_name = 'creditCode');

-- ------------------------------------------------------------
-- 17. 股权变更登记(QY-KB-004)
--    字段: transferRatio(比例%/number) transferAmount(金额万元/number)
--    触发点: 比例越界 / 金额为负 转人工
-- ------------------------------------------------------------
INSERT INTO approval_rule_set (rule_set_name, matter_type, status, logic_type, description)
SELECT '股权变更登记审查', 'QY-KB-004', 1, 'AND', '股权变更:转让比例0~100%+转让金额>0自动核验'
WHERE NOT EXISTS (SELECT 1 FROM approval_rule_set WHERE matter_type = 'QY-KB-004');

INSERT INTO approval_rule_item (rule_set_id, rule_type, field_name, operator, compare_value, logic_operator, sort_order, error_message)
SELECT s.id, 'NUMBER', 'transferRatio', 'BETWEEN', '0,100', 'AND', 1, '转让股权比例应在0~100%之间'
FROM approval_rule_set s WHERE s.matter_type = 'QY-KB-004'
  AND NOT EXISTS (SELECT 1 FROM approval_rule_item i WHERE i.rule_set_id = s.id AND i.field_name = 'transferRatio');

INSERT INTO approval_rule_item (rule_set_id, rule_type, field_name, operator, compare_value, logic_operator, sort_order, error_message)
SELECT s.id, 'NUMBER', 'transferAmount', 'GT', '0', 'AND', 2, '转让金额应大于0'
FROM approval_rule_set s WHERE s.matter_type = 'QY-KB-004'
  AND NOT EXISTS (SELECT 1 FROM approval_rule_item i WHERE i.rule_set_id = s.id AND i.field_name = 'transferAmount');

-- ------------------------------------------------------------
-- 18. 经营期限延期(QY-KB-005)
--    字段: newTerm(延期后期限年/number) extensionReason(说明/textarea)
--    触发点: 延期年限越界 转人工
-- ------------------------------------------------------------
INSERT INTO approval_rule_set (rule_set_name, matter_type, status, logic_type, description)
SELECT '经营期限延期审查', 'QY-KB-005', 1, 'AND', '经营期限延期:延期年限1~50年+延期原因核验'
WHERE NOT EXISTS (SELECT 1 FROM approval_rule_set WHERE matter_type = 'QY-KB-005');

INSERT INTO approval_rule_item (rule_set_id, rule_type, field_name, operator, compare_value, logic_operator, sort_order, error_message)
SELECT s.id, 'NUMBER', 'newTerm', 'BETWEEN', '1,50', 'AND', 1, '延期后经营期限应在1~50年之间'
FROM approval_rule_set s WHERE s.matter_type = 'QY-KB-005'
  AND NOT EXISTS (SELECT 1 FROM approval_rule_item i WHERE i.rule_set_id = s.id AND i.field_name = 'newTerm');

INSERT INTO approval_rule_item (rule_set_id, rule_type, field_name, operator, compare_value, logic_operator, sort_order, error_message)
SELECT s.id, 'TEXT', 'extensionReason', 'CONTAINS', '经营', 'AND', 2, '请填写延期原因(如继续经营)'
FROM approval_rule_set s WHERE s.matter_type = 'QY-KB-005'
  AND NOT EXISTS (SELECT 1 FROM approval_rule_item i WHERE i.rule_set_id = s.id AND i.field_name = 'extensionReason');

-- ============================================================
-- 验证查询
-- ============================================================
SELECT rs.id, rs.rule_set_name, rs.matter_type, rs.logic_type, rs.status,
       COUNT(i.id) AS item_count
FROM approval_rule_set rs
LEFT JOIN approval_rule_item i ON i.rule_set_id = rs.id
GROUP BY rs.id, rs.rule_set_name, rs.matter_type, rs.logic_type, rs.status
ORDER BY rs.matter_type;
