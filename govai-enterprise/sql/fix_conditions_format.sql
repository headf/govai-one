-- 修复 conditions 格式:统一为 JSON 数组,元素用双引号包裹
UPDATE ent_matter_info SET conditions = '["已取得食品经营许可证且在有效期内","按变更事项提交对应材料"]' WHERE id = 5;
UPDATE ent_matter_info SET conditions = '["许可证到期前30日内提出","经营条件未发生重大变化"]' WHERE id = 6;
UPDATE ent_matter_info SET conditions = '["已取得食品经营许可证","决定终止食品经营"]' WHERE id = 7;
UPDATE ent_matter_info SET conditions = '["已取得营业执照","按变更事项提交对应材料"]' WHERE id = 8;
UPDATE ent_matter_info SET conditions = '["未发生债权债务或已清算完结","无税务未办结事项","无社保欠缴"]' WHERE id = 9;
UPDATE ent_matter_info SET conditions = '["上年度未裁员或裁员率低于5%","依法足额缴纳失业保险费满1年"]' WHERE id = 10;
UPDATE ent_matter_info SET conditions = '["已取得卫生许可证且在有效期内","经营条件未发生重大变化"]' WHERE id = 12;
UPDATE ent_matter_info SET conditions = '["已取得食品经营许可证","许可证遗失或损毁"]' WHERE id = 13;
UPDATE ent_matter_info SET conditions = '["已取得营业执照","股权转让协议合法有效"]' WHERE id = 17;
UPDATE ent_matter_info SET conditions = '["已取得营业执照","经营期限即将届满","股东会同意延期"]' WHERE id = 18;

-- 验证
SELECT id, matter_name, conditions FROM ent_matter_info ORDER BY id;
