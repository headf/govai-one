-- 模拟"随便上传"：材料名符合要求，但上传文件名与材料名毫不相关（云启 食品经营许可新办）
INSERT INTO ent_application_material (record_id, material_name, file_url, file_type, original_name, ocr_status, upload_time) VALUES
(102, '法定代表人身份证(OCR)', '/certificate/2026/09/fake1.svg', 'image/svg+xml', 'resume_final_v2.pdf', 0, NOW()),
(102, '从业人员健康证(OCR)', '/certificate/2026/09/fake2.svg', 'image/svg+xml', 'IMG_20260921_001.jpg', 0, NOW()),
(102, '经营场所平面布局图', '/certificate/2026/09/fake3.svg', 'image/svg+xml', 'screenshot.png', 0, NOW()),
(102, '食品安全管理制度', '/certificate/2026/09/fake4.svg', 'image/jpeg', 'IMG_8888.jpg', 0, NOW());
