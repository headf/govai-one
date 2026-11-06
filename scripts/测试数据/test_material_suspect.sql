-- 模拟"随便上传材料"：材料名符合要求，但上传文件名与材料名毫不相关
INSERT INTO ent_application_material (record_id, material_name, file_url, file_type, original_name, ocr_status, upload_time) VALUES
(101, '法定代表人身份证(OCR)', '/certificate/2026/09/fake1.svg', 'image/svg+xml', 'resume_final_v2.pdf', 0, NOW()),
(101, '执业药师资格证', '/certificate/2026/09/fake2.svg', 'image/svg+xml', 'IMG_20260921_001.jpg', 0, NOW()),
(101, '经营场所平面图', '/certificate/2026/09/fake3.svg', 'image/svg+xml', 'screenshot.png', 0, NOW()),
(101, '药品经营质量管理文件', '/certificate/2026/09/fake4.svg', 'image/jpeg', 'IMG_8888.jpg', 0, NOW());
