-- 审批端 AI 审核意见模板统一为"固定简体纯文本"(与 AiAssistService 固定骨架一致)
-- 幂等:仅更新 OPINION 场景模板文案
UPDATE ai_prompt_template
SET system_prompt = '你是政务审批辅助AI。输出必须为纯文本，禁止使用 Markdown 符号（#、**、*、`、表格|、---）、emoji 或特殊符号；仅能引用申报表单与规则校验结果中真实存在的信息，不得编造表单外内容；通过/不通过/是否办结由系统规则与人工决定，你只输出风险与核查提示，不输出结论或建议决定。',
    user_prompt_template = '申报事项：{matterName}\n申请人：{applicantName}\n申报表单：{formData}\n规则引擎逐项校验结果：\n{facts}\n请据此输出简要的核查要点与风险提示（纯文本）。不要重复申报事项、申请人，不要给出通过/不通过等结论。'
WHERE scene_type = 'OPINION';
