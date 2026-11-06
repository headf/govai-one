-- =============================================================
-- 扩展事项 + 虚拟证件(演示用)
-- 1. 新增 8 个事项(id 固定 11-18,便于证照到期联动跳转)
-- 2. 新增 5 个证照(含法人身份证,部分即将到期触发提醒)
-- 3. 把食品经营许可证到期日改近(演示延续跳转)
-- 执行前确保 ent_matter_info / enterprise_license 表已存在
-- =============================================================

-- ---------- 1. 新增事项(id 固定,避免自增不确定) ----------
INSERT IGNORE INTO ent_matter_info
(id, matter_code, matter_name, category_id, department, description, guide_content, conditions, materials, process_steps, deadline_days, form_config, status) VALUES
(11, 'WS-WS-001', '公共场所卫生许可新办', 2, '卫生健康委员会',
 '公共场所经营者需取得卫生许可证方可营业。',
 '适用对象：宾馆、旅店、公共浴室、理发店、美容店等公共场所。办理时限：10个工作日。',
 '["已取得营业执照","经营场所卫生条件达标","从业人员持有有效健康证明"]',
 '["营业执照(电子证照,免证办)","法定代表人身份证(OCR)","从业人员健康证(OCR)","经营场所平面布局图","卫生管理制度"]',
 '["提交申请","形式审查","现场核查(如需)","审批发证"]', 10,
 '[{"key":"enterpriseName","label":"企业名称","type":"text","required":true,"options":null,"ocr":null},{"key":"creditCode","label":"统一社会信用代码","type":"text","required":true,"options":null,"ocr":"BUSINESS_LICENSE"},{"key":"legalPerson","label":"法定代表人","type":"text","required":true,"options":null,"ocr":"BUSINESS_LICENSE"},{"key":"legalPersonIdCard","label":"法定代表人身份证号","type":"text","required":true,"options":null,"ocr":"ID_CARD"},{"key":"placeType","label":"场所类型","type":"select","required":true,"options":["宾馆","旅店","公共浴室","理发店","美容店","其他"],"ocr":null},{"key":"operateAddress","label":"经营场所地址","type":"text","required":true,"options":null,"ocr":null},{"key":"operateArea","label":"经营场所面积(㎡)","type":"number","required":true,"options":null,"ocr":null},{"key":"contactPhone","label":"联系电话","type":"text","required":true,"options":null,"ocr":null}]',
 1),
(12, 'WS-WS-002', '公共场所卫生许可延续', 2, '卫生健康委员会',
 '卫生许可证即将到期(到期前30日内),需延续换证。',
 '适用对象：卫生许可证即将到期的公共场所经营者。办理时限：10个工作日。到期提醒联动：证照库到期前30天自动推送提醒。',
 'null',
 '["原卫生许可证(交回)","营业执照(电子证照,免证办)","从业人员健康证(OCR)"]',
 '["受理","审查","决定","送达"]', 10,
 '[{"key":"enterpriseName","label":"企业名称","type":"text","required":true,"options":null,"ocr":null},{"key":"creditCode","label":"统一社会信用代码","type":"text","required":true,"options":null,"ocr":"BUSINESS_LICENSE"},{"key":"licenseNo","label":"原许可证编号","type":"text","required":true,"options":null,"ocr":null},{"key":"originalExpiryDate","label":"原许可证到期日期","type":"date","required":true,"options":null,"ocr":null},{"key":"operateAddress","label":"经营场所地址","type":"text","required":true,"options":null,"ocr":null},{"key":"placeType","label":"场所类型","type":"select","required":true,"options":["宾馆","旅店","公共浴室","理发店","美容店","其他"],"ocr":null},{"key":"contactPhone","label":"联系电话","type":"text","required":true,"options":null,"ocr":null}]',
 1),
(13, 'SP-JY-005', '食品经营许可补办', 2, '市场监督管理局',
 '食品经营许可证遗失或损毁,需申请补办。',
 '适用对象：许可证遗失或损毁的食品经营者。办理时限：5个工作日。',
 'null',
 '["营业执照(电子证照,免证办)","法定代表人身份证(OCR)","遗失声明或损毁证明"]',
 '["受理","审查","决定","送达"]', 5,
 '[{"key":"enterpriseName","label":"企业名称","type":"text","required":true,"options":null,"ocr":null},{"key":"creditCode","label":"统一社会信用代码","type":"text","required":true,"options":null,"ocr":"BUSINESS_LICENSE"},{"key":"licenseNo","label":"原许可证编号","type":"text","required":true,"options":null,"ocr":null},{"key":"lossReason","label":"补办原因","type":"select","required":true,"options":["遗失","损毁"],"ocr":null},{"key":"lossDetail","label":"情况说明","type":"textarea","required":true,"options":null,"ocr":null},{"key":"contactPhone","label":"联系电话","type":"text","required":true,"options":null,"ocr":null}]',
 1),
(14, 'YC-ZM-001', '烟草专卖零售许可新办', 2, '烟草专卖局',
 '从事烟草制品零售业务需取得烟草专卖零售许可证。',
 '适用对象：拟从事烟草制品零售的经营者。办理时限：8个工作日。',
 '["已取得营业执照","有固定的经营场所"]',
 '["营业执照(电子证照,免证办)","法定代表人身份证(OCR)","经营场所证明"]',
 '["提交申请","形式审查","现场核查","审批发证"]', 8,
 '[{"key":"enterpriseName","label":"企业名称","type":"text","required":true,"options":null,"ocr":null},{"key":"creditCode","label":"统一社会信用代码","type":"text","required":true,"options":null,"ocr":"BUSINESS_LICENSE"},{"key":"legalPerson","label":"法定代表人","type":"text","required":true,"options":null,"ocr":"BUSINESS_LICENSE"},{"key":"legalPersonIdCard","label":"法定代表人身份证号","type":"text","required":true,"options":null,"ocr":"ID_CARD"},{"key":"operateAddress","label":"经营场所地址","type":"text","required":true,"options":null,"ocr":null},{"key":"businessScope","label":"经营范围","type":"textarea","required":true,"options":null,"ocr":null},{"key":"contactPhone","label":"联系电话","type":"text","required":true,"options":null,"ocr":null}]',
 1),
(15, 'YP-JY-001', '药品经营许可新办', 2, '市场监督管理局',
 '从事药品零售经营需取得药品经营许可证。',
 '适用对象：拟从事药品零售的经营者。办理时限：30个工作日。',
 '["已取得营业执照","具备执业药师","经营场所符合规范"]',
 '["营业执照(电子证照,免证办)","法定代表人身份证(OCR)","执业药师资格证","经营场所平面图","药品经营质量管理文件"]',
 '["提交申请","形式审查","现场核查","审批发证"]', 30,
 '[{"key":"enterpriseName","label":"企业名称","type":"text","required":true,"options":null,"ocr":null},{"key":"creditCode","label":"统一社会信用代码","type":"text","required":true,"options":null,"ocr":"BUSINESS_LICENSE"},{"key":"legalPerson","label":"法定代表人","type":"text","required":true,"options":null,"ocr":"BUSINESS_LICENSE"},{"key":"legalPersonIdCard","label":"法定代表人身份证号","type":"text","required":true,"options":null,"ocr":"ID_CARD"},{"key":"pharmacistName","label":"执业药师姓名","type":"text","required":true,"options":null,"ocr":null},{"key":"pharmacistCertNo","label":"执业药师资格证号","type":"text","required":true,"options":null,"ocr":null},{"key":"operateAddress","label":"经营场所地址","type":"text","required":true,"options":null,"ocr":null},{"key":"contactPhone","label":"联系电话","type":"text","required":true,"options":null,"ocr":null}]',
 1),
(16, 'PW-PX-001', '排污许可新办', 2, '生态环境局',
 '依法需要取得排污许可证的排污单位,须申请领取。',
 '适用对象：依法实行排污许可管理的排污单位。办理时限：20个工作日。',
 '["已取得营业执照","建设项目环境影响评价文件已审批"]',
 '["营业执照(电子证照,免证办)","法定代表人身份证(OCR)","环境影响评价批复","排污单位基本信息","污染防治设施说明"]',
 '["提交申请","形式审查","技术审查","审批发证"]', 20,
 '[{"key":"enterpriseName","label":"企业名称","type":"text","required":true,"options":null,"ocr":null},{"key":"creditCode","label":"统一社会信用代码","type":"text","required":true,"options":null,"ocr":"BUSINESS_LICENSE"},{"key":"legalPerson","label":"法定代表人","type":"text","required":true,"options":null,"ocr":"BUSINESS_LICENSE"},{"key":"legalPersonIdCard","label":"法定代表人身份证号","type":"text","required":true,"options":null,"ocr":"ID_CARD"},{"key":"pollutantType","label":"主要污染物","type":"select","required":true,"options":["废水","废气","噪音","固废"],"ocr":null},{"key":"operateAddress","label":"生产经营场所地址","type":"text","required":true,"options":null,"ocr":null},{"key":"contactPhone","label":"联系电话","type":"text","required":true,"options":null,"ocr":null}]',
 1),
(17, 'QY-KB-004', '股权变更登记', 1, '市场监督管理局',
 '有限责任公司股权转让,需办理股权变更登记。',
 '适用对象：股权转让的有限责任公司。办理时限：3个工作日。',
 'null',
 '["营业执照(电子证照,免证办)","股权转让协议","股东会决议","新股东身份证明(OCR)"]',
 '["受理","审查","决定","送达"]', 3,
 '[{"key":"enterpriseName","label":"企业名称","type":"text","required":true,"options":null,"ocr":null},{"key":"creditCode","label":"统一社会信用代码","type":"text","required":true,"options":null,"ocr":"BUSINESS_LICENSE"},{"key":"originalShareholder","label":"原股东姓名","type":"text","required":true,"options":null,"ocr":null},{"key":"newShareholder","label":"新股东姓名","type":"text","required":true,"options":null,"ocr":null},{"key":"newShareholderIdCard","label":"新股东身份证号","type":"text","required":true,"options":null,"ocr":"ID_CARD"},{"key":"transferRatio","label":"转让股权比例(%)","type":"number","required":true,"options":null,"ocr":null},{"key":"transferAmount","label":"转让金额(万元)","type":"number","required":true,"options":null,"ocr":null},{"key":"contactPhone","label":"联系电话","type":"text","required":true,"options":null,"ocr":null}]',
 1),
(18, 'QY-KB-005', '经营期限延期', 1, '市场监督管理局',
 '企业经营期限届满前,需办理延期登记。',
 '适用对象：经营期限即将届满的有限责任公司。办理时限：3个工作日。',
 'null',
 '["营业执照(电子证照,免证办)","股东会决议(同意延期)","修改后的公司章程"]',
 '["受理","审查","决定","送达"]', 3,
 '[{"key":"enterpriseName","label":"企业名称","type":"text","required":true,"options":null,"ocr":null},{"key":"creditCode","label":"统一社会信用代码","type":"text","required":true,"options":null,"ocr":"BUSINESS_LICENSE"},{"key":"originalTerm","label":"原经营期限","type":"text","required":true,"options":null,"ocr":null},{"key":"newTerm","label":"延期后经营期限(年)","type":"number","required":true,"options":null,"ocr":null},{"key":"extensionReason","label":"延期原因","type":"textarea","required":true,"options":null,"ocr":null},{"key":"contactPhone","label":"联系电话","type":"text","required":true,"options":null,"ocr":null}]',
 1);

-- ---------- 2. 新增证照(含法人身份证,部分即将到期) ----------
-- id 固定 5-9,SVG 文件名与 id 对应
INSERT IGNORE INTO enterprise_license
(id, enterprise_id, license_type, license_no, license_name, issue_date, expiry_date, file_url, status) VALUES
-- 企业1(东软智膳,法人李四)
(5, 1, '法人身份证', '110108199001011234', '李四居民身份证', '2020-01-15', '2040-01-15', '/certificate/2026/09/5.svg', 1),
(6, 1, '烟草专卖零售许可证', 'YC11101080008888', '东软智膳餐饮管理有限公司烟草专卖零售许可证', '2024-09-01', '2026-11-01', '/certificate/2026/09/6.svg', 1),
(7, 1, '排污许可证', 'PW11101080007777', '东软智膳餐饮管理有限公司排污许可证', '2023-06-01', '2028-06-01', '/certificate/2026/09/7.svg', 1),
-- 企业2(云启科技,法人王五)
(8, 2, '法人身份证', '110108199202022345', '王五居民身份证', '2021-03-10', '2041-03-10', '/certificate/2026/09/8.svg', 1),
(9, 2, '高新技术企业证书', 'GR202211001234', '云启科技有限公司高新技术企业证书', '2022-12-01', '2030-12-01', '/certificate/2026/09/9.svg', 1);

-- ---------- 3. 把食品经营许可证到期日改近(30天内,演示延续跳转) ----------
UPDATE enterprise_license
SET expiry_date = '2026-10-01'
WHERE license_type LIKE '%食品经营许可%' AND enterprise_id = 1;

-- ---------- 验证 ----------
SELECT id, matter_code, matter_name, category_id FROM ent_matter_info WHERE id BETWEEN 11 AND 18 ORDER BY id;
SELECT id, enterprise_id, license_type, license_no, expiry_date,
       DATEDIFF(expiry_date, CURDATE()) AS days_to_expire
FROM enterprise_license ORDER BY id;
