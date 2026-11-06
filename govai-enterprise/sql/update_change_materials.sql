-- 补充企业变更一件事的材料清单:按变更事项动态条件标注
-- "变更地址时"匹配 changeType 含"地址";"变更法定代表人时"匹配含"法定代表人"
UPDATE ent_matter_info
SET materials = '["营业执照(电子证照,免证办)","变更后经营场所使用证明(变更地址时)","新法定代表人身份证(变更法定代表人时)","决议文件"]'
WHERE matter_code = 'QY-KB-002';

SELECT matter_code, matter_name, materials FROM ent_matter_info WHERE matter_code IN ('QY-KB-002','SP-JY-002');
