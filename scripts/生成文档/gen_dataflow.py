# -*- coding: utf-8 -*-
"""生成《四端数据流与端到端流程说明》"""
from docx import Document
from docx.shared import Pt, RGBColor, Cm
from docx.oxml.ns import qn
from docx.oxml import OxmlElement
from docx.enum.text import WD_ALIGN_PARAGRAPH

F, INK, BLUE, GRAY = '微软雅黑', RGBColor(0x1F,0x2D,0x3D), RGBColor(0x2F,0x6B,0xFF), RGBColor(0x8A,0x8A,0x8A)

doc = Document()
st = doc.styles['Normal']
st.font.name = F; st.font.size = Pt(10)
rpr = st.element.get_or_add_rPr()
rf = rpr.find(qn('w:rFonts'))
if rf is None:
    rf = OxmlElement('w:rFonts'); rpr.append(rf)
rf.set(qn('w:eastAsia'), F)
st.paragraph_format.space_after = Pt(3)


def _f(run, size=10, bold=False, color=INK, font=F):
    run.font.name = font
    run._element.get_or_add_rPr()
    run._element.rPr.rFonts.set(qn('w:eastAsia'), font)
    run.font.size = Pt(size); run.bold = bold
    if color: run.font.color.rgb = color


def title(t):
    p = doc.add_paragraph(); p.alignment = 1
    _f(p.add_run(t), 19, True); p.paragraph_format.space_after = Pt(4)


def sub(t):
    p = doc.add_paragraph(); p.alignment = 1
    _f(p.add_run(t), 9.5, color=GRAY); p.paragraph_format.space_after = Pt(12)


def H1(t):
    p = doc.add_paragraph(); _f(p.add_run(t), 14, True, BLUE)
    p.paragraph_format.space_before = Pt(14); p.paragraph_format.space_after = Pt(5)


def H2(t):
    p = doc.add_paragraph(); _f(p.add_run(t), 11.5, True)
    p.paragraph_format.space_before = Pt(9); p.paragraph_format.space_after = Pt(3)


def P(t):
    p = doc.add_paragraph(); _f(p.add_run(t)); return p


def NOTE(t):
    p = doc.add_paragraph(); p.paragraph_format.left_indent = Pt(10)
    _f(p.add_run(t), 9, color=GRAY, font=F); return p


def TB(headers, rows, widths=None):
    t = doc.add_table(rows=1, cols=len(headers)); t.style = 'Light Grid Accent 1'
    for i, h in enumerate(headers):
        c = t.rows[0].cells[i]; c.text = ''
        _f(c.paragraphs[0].add_run(h), 9.5, True)
    for r in rows:
        cs = t.add_row().cells
        for i, v in enumerate(r):
            cs[i].text = ''
            _f(cs[i].paragraphs[0].add_run(str(v)), 9)
    return t


# ============ 封面 ============
title('GovAI One 智慧政务系统')
sub('四端数据流与端到端流程说明 —— 数据从哪来、调了什么、写到哪')

# ============ 一、总体主线 ============
H1('一、总体数据主线（一条链路贯穿四端）')
TB(['步', '谁触发', '发生了什么', '用到的技术 / 数据'], [
    ['1', '群众端 / 企业端', '提交申报（表单 + 材料）', 'Vue3 前端 → Spring Boot → 校验后落申报表'],
    ['2', '审批端', '规则引擎自动校验（判断过 / 不过 / 转人工）', '规则集表 + 规则项表 → 规则执行器'],
    ['3', '审批端', '大模型生成核查要点与风险提示', 'LangChain4j → DeepSeek → AI 对话记录表'],
    ['4', '审批端', '按岗位流转：受理初审 → 终审决定 → 办结出证', '审批实例表 + 任务表（岗位快照 + CAS 条件更新）'],
    ['5', '审批端', '办结后回调申报方（出证 / 站内信）', 'OpenFeign 同步回调（企业端出证、群众端通知）'],
    ['6', '监管端', '大屏实时展示 + 信用与预警联动', 'RabbitMQ 异步推送 → 审批缓存表 → WebSocket 推送前端'],
])
NOTE('核心原则：跨端只走接口（Feign 同步 / MQ 异步），不允许跨库直接查表——四个库各自独立 Schema。')

# ============ 二、认证链路 ============
H1('二、认证链路（SSO 如何贯穿四端）')
TB(['端', '登录入口', '数据来源', '技术'], [
    ['监管端', '账号 + 密码（可选登录端）', 'govai_supervision.sys_user', 'Spring Security 校验 + BCrypt 密码比对'],
    ['审批端', '账号 + 密码（同监管端账号）', '通过 Feign 调监管端认证中心', 'Feign → 监管端签发 JWT'],
    ['企业端', '企业账号 + 密码', 'govai_enterprise.enterprise_user', '自签 JWT（共享密钥）'],
    ['群众端', '手机号 + 验证码（演示固定码 000000）', 'govai_citizen.citizen_user + 监管端 sys_user', 'Feign → 监管端 sms-login 签发 JWT'],
])
NOTE('JWT 里携带 userId / role / enterpriseId；各端拦截器统一校验，实现"登录一次、四端通用"。')

# ============ 三、群众端 ============
H1('三、群众端流程（微信小程序 + H5）')
TB(['流程', '数据从哪来', '调用什么', '写到哪'], [
    ['手机号登录', 'citizen_user（未注册自动注册）', 'Feign → 监管端 /auth/sms-login', '返回 JWT（token 存本地）'],
    ['事项浏览 / 检索', 'matter_info + matter_category', '本端接口（分页 + 关键词）', '只读'],
    ['在线申报提交', '用户填写表单', 'Feign → 审批端 /approval/submit', 'citizen.application_record（记 approval_instance_id）'],
    ['申报材料上传', '用户上传图片', '本端上传接口（文件落 upload 目录）', 'application_material'],
    ['进度查询', '审批端实例 + 任务', 'Feign → 审批端 /approval/progress/{id}', '只读'],
    ['AI 办事助手', '事项指南 + 政策内容（关键词检索）', 'Feign → 审批端 /ai/chat（DeepSeek）', 'AI 对话记录 + qa 会话'],
    ['办结通知', '审批端回调推送', '审批端 Feign → /external/records/status', 'progress_notification + site_message'],
    ['好差评', '用户打分（1–5 星 + 标签 + 文字）', '本端评价接口；差评(≤2星) Feign → 监管端上报', 'evaluation；差评另写 review_order（整改工单）'],
])

# ============ 四、企业端 ============
H1('四、企业端流程（PC 门户）')
TB(['流程', '数据从哪来', '调用什么', '写到哪'], [
    ['企业登录', 'enterprise_user', '本端认证（自签 JWT）', '返回 token'],
    ['办事大厅', 'ent_matter_info + ent_matter_category', '本端接口（分类筛选 + 搜索 + 分页）', '只读'],
    ['智能办事规划', '用户一句自然语言描述', '大模型（生成所需事项清单）', 'ent_matter_plan_history'],
    ['表单预填', 'enterprise_info（企业档案）', '本端预填接口', '表单初值'],
    ['OCR 识别回填', '营业执照照片', '调 OCR 服务（PaddleOCR:8090）', '回填表单字段'],
    ['免证办核验', 'enterprise_license（有效电子证照）', '本端材料清单计算', '材料免上传标记'],
    ['材料上传', '用户上传文件', '本端上传接口（校验类型/大小 + 保存原文件名）', 'ent_application_material'],
    ['申报提交', '表单 + 材料', '三层校验（材料齐全性 / 重复发证 / 信用门禁）→ Feign 调审批端', 'ent_application_record（PENDING→APPROVING）'],
    ['上报监管端', '申报记录', 'Feign → 监管端 /ent-view/approvals/report', 'approval_data_cache（大屏数据源）'],
    ['办结回调出证', '审批端回调（PASSED/REJECTED/REPAIR）', '审批端 Feign → /approval-result-by-instance', 'enterprise_license（签发/作废）+ 状态更新'],
    ['政策匹配 / 补贴', '企业画像 + 政策库', '本端匹配逻辑', 'ent_policy_match / subsidy_payment'],
    ['好差评', '用户评价', '本端评价接口；差评 Feign → 监管端上报', 'ent_evaluation；差评写 review_order'],
])

# ============ 五、审批端 ============
H1('五、审批端流程（PC 工作台）')
TB(['流程', '数据从哪来', '调用什么', '写到哪'], [
    ['登录', '监管端账号体系', 'Feign → 监管端认证中心', '返回 JWT'],
    ['待办列表', 'approval_task（按岗位过滤）', '本端接口（按 process_role_code 过滤）', '只读'],
    ['接收申报（受理）', '群众/企业端提交的表单', '外部 Feign → /approval/submit', 'approval_instance + 首个节点 task'],
    ['规则引擎校验', '规则集 + 规则项 + 表单值', '规则执行器（数值/文本/证照，AND-OR 组合）', '实例状态（PASS 可快进办结岗）'],
    ['AI 审核意见', '表单原文 + 规则逐项结果', 'LangChain4j → DeepSeek（后台异步预生成）', 'ai_chat_record + 审计日志（AI 建议）'],
    ['多岗位流转', '任务 + 岗位快照', 'CAS 条件更新（仅"待处理"可更新）', 'approval_task + approval_instance 状态'],
    ['办结（终态）', '办结岗 PASS', 'Feign 双回调：企业端出证 / 群众端通知', '实例置 APPROVED + 记 finish_time'],
    ['推送监管端', '实例最新状态', 'RabbitMQ → approval.exchange → approval.record.queue', '监管端 approval_data_cache'],
    ['审计留痕', '每一次关键操作', '本端写审计日志（AI 建议与人工决定分开记）', 'audit_log'],
    ['配置管理', '管理员配置', '流程模板/节点、规则集/规则项、AI 提示词模板', 'approval_process_template / node / rule_set / rule_item / ai_prompt_template'],
])

# ============ 六、监管端 ============
H1('六、监管端流程（大屏 + 后台）')
TB(['流程', '数据从哪来', '调用什么', '写到哪'], [
    ['登录（认证中心）', 'sys_user + 角色权限', '本端认证（SSO 中心，为四端签发 JWT）', '返回 JWT + 权限码'],
    ['数据大屏', '审批端 MQ 推送 + 企业/群众端上报', 'RabbitMQ 消费 + WebSocket 主动推送', 'approval_data_cache（消费写入）'],
    ['双随机抽查', '审批实例池', '随机抽取（可选按信用加权：D 级优先）+ 定向抽查', 'supervision_sample'],
    ['风险预警', '风险规则表', '规则扫描 → 生成预警 → 处置流程', 'risk_rule → risk_alert'],
    ['信用体系', '企业/个人数据 + 评价与违规', '评分模型 + 等级映射 + 修复审核', 'credit_enterprise / credit_personal / credit_repair'],
    ['差评整改', '群众端/企业端差评上报（Feign）', '接收上报 → 生成工单 → 处置', 'review_order'],
    ['审计追溯', '审批缓存 + 抽查 + 预警 + 审计留痕', '按实例号聚合全链路（/audit/traces/chain/{id}）', 'audit_trace（聚合查询，不额外落库）'],
    ['操作日志', '所有后台管理操作', 'Spring AOP 切面自动记录', 'sys_log'],
    ['系统管理', '管理员维护', '用户 / 角色 / 权限（RBAC）/ 数据字典', 'sys_user / sys_role / sys_permission / sys_dict'],
])

# ============ 七、跨端技术清单 ============
H1('七、跨端技术清单')
H2('7.1 同步接口契约（OpenFeign）')
TB(['#', '提供方', '消费方', '路径', '用途'], [
    ['1', '监管端', '四端', 'POST /api/v1/auth/login', '统一登录'],
    ['2', '监管端', '全部', 'GET /api/v1/auth/verify', 'Token 验证'],
    ['3', '群众端', '审批端', 'GET /api/v1/matters/{id}', '查事项信息'],
    ['4', '审批端', '群众 / 企业端', 'POST /api/v1/approval/submit', '提交审批（创建实例）'],
    ['5', '审批端', '群众 / 企业端', 'GET /api/v1/approval/progress/{id}', '查审批进度'],
    ['6', '审批端', '群众端', 'POST /api/v1/ai/chat', '大模型对话'],
    ['7', '审批端', '监管端', 'POST /api/v1/ai/summary', '数据摘要'],
    ['8', '企业端', '审批端', 'POST /api/v1/ocr/recognize', 'OCR 识别（营业执照回填）'],
    ['9', '企业端', '审批端', 'POST /api/v1/ent-apply/approval-result-by-instance', '审批定稿回调（出证）'],
    ['10', '群众 / 企业端', '监管端', 'POST /api/v1/review-orders/report', '差评上报（生成整改工单）'],
])
H2('7.2 异步通道（RabbitMQ）')
TB(['交换机 / 队列', '生产者', '消费者', '数据', '落库'], [
    ['approval.exchange → approval.record.queue', '审批端', '监管端', '审批实例办理数据（含状态、耗时）', '监管端 approval_data_cache'],
])
H2('7.3 数据存储')
TB(['类型', '内容'], [
    ['MySQL 8', '四库 51 张表（群众 9 / 审批 9 / 企业 17 / 监管 16），逻辑隔离、跨端只走接口'],
    ['Redis', '缓存与临时数据（未启动时业务代码自动降级，不阻塞主流程）'],
    ['本地文件', '企业端 upload 目录（材料 / 证照图片），通过 /files/** 静态映射访问'],
    ['大模型', 'DeepSeek（经 LangChain4j 接入，模型可替换）'],
    ['OCR', 'PaddleOCR HTTP 服务（8090，用于营业执照识别回填）'],
])

# ============ 八、数据表归属 ============
H1('八、数据表归属（谁的数据谁管）')
TB(['Schema', '表数', '归属端', '主要表'], [
    ['govai_citizen', '9', '群众端', '群众用户 / 事项 / 申报 / 材料 / 通知 / 消息 / 评价 / 政策推荐'],
    ['govai_approval', '9', '审批端', '流程模板 / 节点 / 实例 / 任务 / 规则集 / 规则项 / 审计日志 / Prompt / AI 对话'],
    ['govai_enterprise', '17', '企业端', '企业用户 / 企业信息 / 法人事项 / 申报 / 材料 / 电子证照 / 合规预检 / 政策匹配 / 套餐 / 问答 / 规划历史 / 经办人 / 补贴'],
    ['govai_supervision', '16', '监管端', '用户 / 角色 / 权限 / 字典 / 日志 / 企业信用 / 个人信用 / 信用修复 / 风险规则 / 风险预警 / 抽查 / 差评工单 / 审计追溯 / 日统计 / 审批缓存'],
])
NOTE('跨库禁止直接查表：需要别的端的数据时，一律调用对方接口（Feign）。')

out = r'D:\Desktop\企业实训-智能政务系统\文档\GovAI-One-四端数据流与端到端流程说明.docx'
doc.save(out)
print('已生成：', out)
