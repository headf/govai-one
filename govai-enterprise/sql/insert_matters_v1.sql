-- =============================================================
-- V1.0 企业端最小事项清单 · 新增事项数据
-- 覆盖清单第一优先级:食品许可闭环(变更/延续/注销)+市场主体闭环(企业变更/简易注销)
-- 执行方式:连接 govai_enterprise 库后执行本脚本(可重复,带 matter_code 唯一保护)
-- =============================================================

-- 1. 食品经营许可变更(SP-JY-002, category=2 经营许可)
INSERT INTO ent_matter_info (matter_code, matter_name, category_id, department, description, guide_content, conditions, materials, process_steps, deadline_days, form_config, status)
SELECT 'SP-JY-002', '食品经营许可变更', 2, '市场监督管理局',
  '已取得食品经营许可证,需变更经营地址/经营项目/负责人的市场主体。',
  '适用对象：已取得食品经营许可证,需变更经营地址/经营项目/负责人的市场主体。办理时限：5个工作日。',
  '1.已取得食品经营许可证且在有效期内；2.按变更事项提交对应材料。',
  '["原食品经营许可证(交回)","变更后经营场所使用证明(变更地址时)","变更后负责人身份证(OCR,变更负责人时)"]',
  '["受理","审查","决定","送达"]', 5,
  '[{"key":"enterpriseName","label":"企业名称","type":"text","required":true},{"key":"creditCode","label":"统一社会信用代码","type":"text","required":true,"ocr":"BUSINESS_LICENSE"},{"key":"licenseNo","label":"原许可证编号","type":"text","required":true},{"key":"changeType","label":"变更事项","type":"select","required":true,"options":["经营地址变更","经营项目变更","负责人变更"]},{"key":"newOperateAddress","label":"变更后经营场所地址","type":"text","required":false},{"key":"newFoodCategory","label":"变更后经营项目","type":"select","required":false,"options":["热食类食品制售","冷食类食品制售","预包装食品销售","散装食品销售","糕点类食品制售"]},{"key":"newLegalPerson","label":"变更后负责人","type":"text","required":false},{"key":"newLegalPersonIdCard","label":"变更后负责人身份证号","type":"text","required":false,"ocr":"ID_CARD"},{"key":"contactPhone","label":"联系人电话","type":"text","required":true}]',
  1
WHERE NOT EXISTS (SELECT 1 FROM ent_matter_info WHERE matter_code='SP-JY-002');

-- 2. 食品经营许可延续(SP-JY-003)
INSERT INTO ent_matter_info (matter_code, matter_name, category_id, department, description, guide_content, conditions, materials, process_steps, deadline_days, form_config, status)
SELECT 'SP-JY-003', '食品经营许可延续', 2, '市场监督管理局',
  '食品经营许可证即将到期(到期前30日内),需延续换证的市场主体。',
  '适用对象：食品经营许可证即将到期(到期前30日内),需延续换证的市场主体。办理时限：10个工作日。到期提醒联动：证照库到期前30天自动推送提醒。',
  '1.许可证到期前30日内提出；2.经营条件未发生重大变化。',
  '["原食品经营许可证(交回)","营业执照(电子证照,免证办)"]',
  '["受理","审查","决定","送达"]', 10,
  '[{"key":"enterpriseName","label":"企业名称","type":"text","required":true},{"key":"creditCode","label":"统一社会信用代码","type":"text","required":true,"ocr":"BUSINESS_LICENSE"},{"key":"licenseNo","label":"原许可证编号","type":"text","required":true},{"key":"originalExpiryDate","label":"原许可证到期日期","type":"date","required":true},{"key":"operateAddress","label":"经营场所地址","type":"text","required":true},{"key":"operateArea","label":"经营场所面积(㎡)","type":"number","required":true},{"key":"foodCategory","label":"经营项目","type":"select","required":true,"options":["热食类食品制售","冷食类食品制售","预包装食品销售","散装食品销售","糕点类食品制售"]},{"key":"contactPhone","label":"联系人电话","type":"text","required":true}]',
  1
WHERE NOT EXISTS (SELECT 1 FROM ent_matter_info WHERE matter_code='SP-JY-003');

-- 3. 食品经营许可注销(SP-JY-004)
INSERT INTO ent_matter_info (matter_code, matter_name, category_id, department, description, guide_content, conditions, materials, process_steps, deadline_days, form_config, status)
SELECT 'SP-JY-004', '食品经营许可注销', 2, '市场监督管理局',
  '终止食品经营的市场主体。',
  '适用对象：终止食品经营的市场主体。办理时限：5个工作日。需交回原许可证。',
  '1.已取得食品经营许可证；2.决定终止食品经营。',
  '["原食品经营许可证(交回)"]',
  '["受理","审查","决定","送达"]', 5,
  '[{"key":"enterpriseName","label":"企业名称","type":"text","required":true},{"key":"creditCode","label":"统一社会信用代码","type":"text","required":true,"ocr":"BUSINESS_LICENSE"},{"key":"licenseNo","label":"原许可证编号","type":"text","required":true},{"key":"cancelReason","label":"注销原因","type":"select","required":true,"options":["终止经营","转行","其他"]},{"key":"cancelReasonDetail","label":"注销情况说明","type":"textarea","required":true},{"key":"contactPhone","label":"联系人电话","type":"text","required":true}]',
  1
WHERE NOT EXISTS (SELECT 1 FROM ent_matter_info WHERE matter_code='SP-JY-004');

-- 4. 企业变更一件事(QY-KB-002, category=1 市场主体)
INSERT INTO ent_matter_info (matter_code, matter_name, category_id, department, description, guide_content, conditions, materials, process_steps, deadline_days, form_config, status)
SELECT 'QY-KB-002', '企业变更一件事', 1, '市场监督管理局',
  '企业名称/法定代表人/经营地址/经营范围/注册资本发生变更的内资有限公司,一表申请,多部门联办。',
  '适用对象：企业名称/法定代表人/经营地址/经营范围/注册资本发生变更的内资有限公司。办理时限：3个工作日(联办)。变更通过后自动同步更新证照库和企业画像。',
  '1.已取得营业执照；2.按变更事项提交对应材料。',
  '["营业执照(电子证照,免证办)","变更后经营场所使用证明(变更地址时)","决议文件"]',
  '["受理","审查","决定","送达"]', 3,
  '[{"key":"enterpriseName","label":"原企业名称","type":"text","required":true},{"key":"creditCode","label":"统一社会信用代码","type":"text","required":true,"ocr":"BUSINESS_LICENSE"},{"key":"changeType","label":"变更事项","type":"select","required":true,"options":["企业名称变更","法定代表人变更","经营地址变更","经营范围变更","注册资本变更"]},{"key":"newEnterpriseName","label":"变更后企业名称","type":"text","required":false},{"key":"newLegalPerson","label":"变更后法定代表人","type":"text","required":false,"ocr":"ID_CARD"},{"key":"newRegisterAddress","label":"变更后注册地址","type":"text","required":false},{"key":"newBusinessScope","label":"变更后经营范围","type":"textarea","required":false},{"key":"newRegisterCapital","label":"变更后注册资本(万元)","type":"number","required":false},{"key":"contactPhone","label":"经办人联系电话","type":"text","required":true}]',
  1
WHERE NOT EXISTS (SELECT 1 FROM ent_matter_info WHERE matter_code='QY-KB-002');

-- 5. 企业简易注销一件事(QY-KB-003)
INSERT INTO ent_matter_info (matter_code, matter_name, category_id, department, description, guide_content, conditions, materials, process_steps, deadline_days, form_config, status)
SELECT 'QY-KB-003', '企业简易注销一件事', 1, '市场监督管理局',
  '未发生债权债务或已清算完结的有限公司,营业执照注销+税务清税+社保注销联办。',
  '适用对象：未发生债权债务或已清算完结的有限公司。办理时限：1个工作日(联办,营业执照注销+税务清税+社保注销)。完成企业生命周期最后一环。',
  '1.未发生债权债务或已清算完结；2.无税务未办结事项；3.无社保欠缴。',
  '["营业执照正副本(交回)","清算报告","税务清税证明","社保注销证明"]',
  '["受理","审查","决定","送达"]', 1,
  '[{"key":"enterpriseName","label":"企业名称","type":"text","required":true},{"key":"creditCode","label":"统一社会信用代码","type":"text","required":true,"ocr":"BUSINESS_LICENSE"},{"key":"cancelReason","label":"注销原因","type":"select","required":true,"options":["决议解散","合并分立解散","章程规定营业期限届满","其他"]},{"key":"liquidationComplete","label":"清算是否完结","type":"select","required":true,"options":["已清算完结","未清算"]},{"key":"noTaxIssue","label":"是否无税务未办结事项","type":"select","required":true,"options":["是","否"]},{"key":"cancelDetail","label":"注销情况说明","type":"textarea","required":true},{"key":"contactPhone","label":"经办人联系电话","type":"text","required":true}]',
  1
WHERE NOT EXISTS (SELECT 1 FROM ent_matter_info WHERE matter_code='QY-KB-003');

-- 6. 餐饮业稳岗返还补贴申报(SP-BZ-002, category=4 政策补贴)
-- 用途:政策页"免申即享确认"后,提供"去申报兑现"入口,形成匹配-确认-申报-兑现完整链路
INSERT INTO ent_matter_info (matter_code, matter_name, category_id, department, description, guide_content, conditions, materials, process_steps, deadline_days, form_config, status)
SELECT 'SP-BZ-002', '餐饮业稳岗返还补贴申报', 4, '人力资源和社会保障局',
  '餐饮业符合稳岗返还条件的企业,免申即享匹配后,可在此申报兑现。',
  '适用对象：餐饮业符合稳岗返还条件的企业。办理时限：10个工作日。免申即享匹配后,可在此申报兑现,补贴直接拨付企业账户。',
  '1.上年度未裁员或裁员率低于5%；2.依法足额缴纳失业保险费满1年。',
  '["营业执照(电子证照,免证办)","上年度失业保险缴费凭证","银行开户许可证"]',
  '["受理","审核","公示","拨付"]', 10,
  '[{"key":"enterpriseName","label":"企业名称","type":"text","required":true},{"key":"creditCode","label":"统一社会信用代码","type":"text","required":true,"ocr":"BUSINESS_LICENSE"},{"key":"employeeCount","label":"用工人数","type":"number","required":true},{"key":"lastYearInsuranceAmount","label":"上年度失业保险缴费金额(元)","type":"number","required":true},{"key":"bankAccount","label":"收款银行账号","type":"text","required":true},{"key":"bankName","label":"开户银行","type":"text","required":true},{"key":"contactPhone","label":"联系人电话","type":"text","required":true}]',
  1
WHERE NOT EXISTS (SELECT 1 FROM ent_matter_info WHERE matter_code='SP-BZ-002');

-- 验证
SELECT id, matter_code, matter_name, category_id, department, deadline_days FROM ent_matter_info ORDER BY id;
