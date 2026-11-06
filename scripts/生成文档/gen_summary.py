# -*- coding: utf-8 -*-
"""汇总：系统原理问答集 + 27 页逐页讲稿 → 合并为一份文档"""
import os
from docx import Document
from docx.shared import Pt, RGBColor
from docx.oxml.ns import qn
from docx.oxml import OxmlElement
from docx.enum.text import WD_ALIGN_PARAGRAPH

FONT = '微软雅黑'
INK = RGBColor(0x1F, 0x2D, 0x3D)
BLUE = RGBColor(0x2F, 0x6B, 0xFF)
GRAY = RGBColor(0x8A, 0x8A, 0x8A)
GREEN = RGBColor(0x1A, 0x7F, 0x4B)
RED = RGBColor(0xC0, 0x39, 0x2B)

SRC_SPEECH = r'D:\Desktop\企业实训-智能政务系统\文档\答辩\GovAI-One-逐页讲稿(27页结题答辩).docx'
OUT = r'D:\Desktop\企业实训-智能政务系统\文档\答辩\GovAI-One-答辩汇总(原理问答+逐页讲稿).docx'

doc = Document()
st = doc.styles['Normal']
st.font.name = FONT
st.font.size = Pt(10.5)
rpr = st.element.get_or_add_rPr()
rf = rpr.find(qn('w:rFonts'))
if rf is None:
    rf = OxmlElement('w:rFonts'); rpr.append(rf)
rf.set(qn('w:eastAsia'), FONT)
st.paragraph_format.line_spacing = 1.4
st.paragraph_format.space_after = Pt(3)


def _f(run, size=10.5, bold=False, color=INK):
    run.font.name = FONT
    run._element.get_or_add_rPr()
    run._element.rPr.rFonts.set(qn('w:eastAsia'), FONT)
    run.font.size = Pt(size)
    run.bold = bold
    if color is not None:
        run.font.color.rgb = color


def TITLE(t):
    p = doc.add_paragraph(); p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    _f(p.add_run(t), 20, True)
    p.paragraph_format.space_after = Pt(4)


def SUB(t):
    p = doc.add_paragraph(); p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    _f(p.add_run(t), 10, color=GRAY)
    p.paragraph_format.space_after = Pt(14)


def H1(t):
    p = doc.add_paragraph(); _f(p.add_run(t), 15, True, BLUE)
    p.paragraph_format.space_before = Pt(16); p.paragraph_format.space_after = Pt(6)


def H2(t):
    p = doc.add_paragraph(); _f(p.add_run(t), 12, True)
    p.paragraph_format.space_before = Pt(10); p.paragraph_format.space_after = Pt(3)


def P(t):
    p = doc.add_paragraph(); _f(p.add_run(t)); return p


def Q(t):
    p = doc.add_paragraph(); _f(p.add_run(t), 11.5, True, RGBColor(0xB4, 0x5C, 0x00))
    p.paragraph_format.space_before = Pt(10); p.paragraph_format.space_after = Pt(3)
    return p


def A(t):
    p = doc.add_paragraph()
    _f(p.add_run('答：' + t), color=GREEN)
    return p


def B(t):
    p = doc.add_paragraph(); p.paragraph_format.left_indent = Pt(16)
    _f(p.add_run('· ' + t)); return p


def WARN(t):
    p = doc.add_paragraph(); p.paragraph_format.left_indent = Pt(12)
    _f(p.add_run('⚠ ' + t), 10, True, RED); return p


def TABLE(headers, rows):
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


# ==================== 封面 ====================
TITLE('GovAI One 智慧政务系统')
SUB('答辩汇总材料 —— 系统原理问答集 + 27 页逐页讲稿')
P('本文件两部分：')
B('第一部分：系统原理问答集（AI 知识库 / AI 智能审核原理 / 四端数据流 / 监管模块用途 / 证照与材料校验）')
B('第二部分：27 页结题答辩 PPT 逐页讲稿（照念稿 + 英文术语注解 + 技术说明）')

doc.add_page_break()

# ==================== 第一部分：问答集 ====================
H1('第一部分  系统原理问答集')

H2('Q1：AI 用了什么知识库？')
A('我们的 AI 没有独立的知识库系统，也没有使用向量数据库或知识图谱。"知识库"实际上就是业务数据库里的表。')
B('知识来源：事项表（事项名 + 办理指南 + 申报条件）与政策表（政策标题 + 政策内容）')
B('检索方式：关键词包含匹配（把问题里的词与事项名/政策标题/政策内容做包含判断），命中后把内容拼成上下文')
B('生成方式：上下文 + 用户问题 → 交给大模型（DeepSeek）组织语言回答')
B('会话记忆：多轮上下文存在 MySQL 会话表（取最近几轮），不是向量记忆')
B('兜底：大模型失败时切换到代码里硬编码的本地常见问答（FAQ）')
P('为什么不上向量库：当前知识量不大（几十个事项、若干政策）、领域窄，关键词检索已经够用；引入 embedding + 向量库会显著增加部署复杂度而收益有限。如果以后知识规模变大、同义表达变多（例如搜"开餐馆"要命中"餐饮服务许可"），再升级为语义检索。')
WARN('答辩时绝对不要说用了 Neo4j / Milvus / 知识图谱——技术方案文档里写过，但代码里没有实现，一追问细节就会露馅。')

H2('Q2：AI 智能审核是怎么审核的？什么原理？')
A('"AI 智能审核"其实是两层，各管一段：规则引擎负责判断"过不过"，大模型只负责写"核查要点"。结论永远不由 AI 产生。')
P('第一层：规则引擎（确定性判断，提交时立即执行）')
B('输入：申报表单数据 + 该事项配置的规则集')
B('规则类型：数值比较（NUMBER）/ 文本匹配（TEXT）/ 日期判断（DATE）/ 证照库查询（LICENSE）')
B('操作符：GT（大于）/ LT（小于）/ EQ（等于）/ CONTAINS（包含）/ BETWEEN（区间），规则项之间支持 AND / OR 组合')
B('实例：食品经营许可新办配了三条规则——经营面积 GT 5、经营项目 CONTAINS 食品、营业执照 LICENSE EQ true')
B('输出三种结论：通过（全部规则通过）/ 未通过（存在未过项）/ 转人工（未配规则或需人工判定）')
B('流转：结论为"通过"时，系统快进到办结出证岗等待人工复核（不自动办结）；"未通过"或"转人工"则进入受理初审岗')
P('第二层：大模型生成核查要点（提交后后台异步预生成）')
B('输入给模型的是：申报事项、申请人、申报表单原文、规则逐项校验结果')
B('系统提示词强约束：输出必须为纯文本、禁止 Markdown/emoji/特殊符号、核查要点逐条另起一行且每条不超过 25 字、只能引用表单里真实存在的信息、不得编造、不输出结论或建议决定')
B('模型输出后经文本清洗（去符号、折空行），再拼进固定骨架')
P('最终的审核意见是固定五段：')
B('申报事项：…（系统填）')
B('申请人：…（系统填）')
B('规则校验结论：通过 / 未通过 / 转人工（由规则引擎结果决定，不是 AI 写的）')
B('建议决定：…（系统按规则结果映射，不是 AI 写的）')
B('AI 核查要点：…（这一段才是大模型写的）')
P('设计的底层原理：让"确定性的判断"交给规则，因为它可解释（能说清卡在哪一条）、可复现、可审计；让"开放性的提示"交给大模型，因为它能理解材料并给出风险线索；但最终结论必须由人做出——政务审批是要负法律责任的行政决定，必须可追溯、责任到人。审计日志中，"AI 建议"与"人工决定"也是分开记录的。')
P('当前的局限（如实说明）：AI 看不到申报材料的内容，只拿到表单与规则结果，因此它只能基于表单提示；它也没有改变结论的能力。')

H2('Q3：四端的数据是怎么流转的？调用了什么技术？')
P('一条主线贯穿四端：申报提交 → 规则引擎校验 → AI 生成要点 → 多岗位流转 → 办结回调 → 监管展示。')
TABLE(['步', '谁触发', '发生了什么', '技术 / 数据'], [
    ['1', '群众端 / 企业端', '提交申报（表单 + 材料）', 'Vue 前端 → Spring Boot → 校验后落申报表'],
    ['2', '审批端', '规则引擎自动校验', '规则集表 + 规则项表 → 规则执行器'],
    ['3', '审批端', '大模型生成核查要点', 'LangChain4j → DeepSeek → AI 对话记录表'],
    ['4', '审批端', '按岗位流转（受理→终审→办结）', '审批实例表 + 任务表（岗位快照 + CAS 条件更新）'],
    ['5', '审批端', '办结后回调申报方', 'OpenFeign 同步回调（企业端出证 / 群众端站内信）'],
    ['6', '监管端', '大屏实时展示 + 预警联动', 'RabbitMQ 异步推送 → 审批缓存表 → WebSocket 推送'],
])
P('跨端技术清单：')
B('同步接口契约（OpenFeign）：统一登录、Token 验证、事项查询、提交审批、查询进度、大模型对话、数据摘要、OCR 识别、审批定稿回调、差评上报')
B('异步通道（RabbitMQ）：approval.exchange → approval.record.queue，审批端推送审批数据，监管端消费后写入审批缓存表')
B('数据存储：MySQL 四库共 51 张表（群众 9 / 审批 9 / 企业 17 / 监管 16），逻辑隔离；Redis 做缓存；本地文件存材料与证照图片')
B('AI / OCR：LangChain4j 接 DeepSeek；PaddleOCR 服务用于营业执照识别回填')
P('四库的归属原则是"谁的数据谁管"：跨库禁止直接查表，需要别的端的数据时一律调用对方接口——这是政务数据"最小可见"原则的落地。')

H2('Q4：监督检查和审计追溯有什么用？')
P('这两个模块定位完全不同：监督检查是"主动出击查一批"，审计追溯是"事后回溯查一件"。')
TABLE(['', '监督检查（双随机抽查）', '审计追溯'], [
    ['视角', '一批（从办过的件里抽样 N 件）', '一件（按审批实例号回溯单件）'],
    ['方向', '主动巡查', '事后回溯'],
    ['目的', '发现风险', '说清责任'],
    ['实现', '随机抽对象 + 随机派人，支持按信用差异化抽取（D 级企业最优先）', '按实例号聚合审批 / 抽查 / 预警 / 审计四个域的痕迹'],
    ['数据', '抽查记录表（有真实数据）', '聚合查询（审批缓存 + 抽查 + 预警 + 审计留痕）'],
])
P('监督检查的价值：避免"想查谁就查谁"的选择性执法，同时把有限的监管力量投向高风险对象（信用等级越低越优先被抽到）。')
P('审计追溯的价值：用于责任认定与合规举证——给定一个实例号，能一次看清"这件为什么批了、谁在什么时候批的、有没有被抽查过、有没有触发过预警"。')

H2('Q5：电子证照的生命周期是怎么管的？')
A('证照不是简单的增删改查，而是围绕业务场景形成的状态机，覆盖五种场景。')
TABLE(['业务场景', '证照变化'], [
    ['新办', '签发一张新证照（有效）；已有同类型有效证照时禁止重复新办'],
    ['延续（换发）', '旧证作废（标记为延续换发）、签发新证，旧证进入"证照历史"区'],
    ['变更（换发）', '同上，作废原因标记为变更换发'],
    ['注销', '证照置为已失效，按作废原因区分：自行注销 / 企业注销，并进入"已注销"区'],
    ['补办', '证照遗失时重新签发'],
])
P('配套的防护：注销 / 延续 / 变更提交时会校验"目标证照必须有效"（拿已注销证号提交会被拦截并提示）；新办会校验重复发证；同类型多张有效证照时，卡片上会明确标注"同类型共 N 张"。')

H2('Q6：材料上传有哪些校验？')
P('材料校验分三层（这是逐步补齐的）：')
B('第一层 上传时：校验文件类型（仅 JPG / PNG / PDF）与大小（≤10MB），挡住空文件、超大文件、非图片 PDF 的无效材料；服务端强制（前端的 accept 只是选择器过滤、可被绕过）')
B('第二层 提交时：校验材料齐全性（清单中每份材料要么已上传、要么免证办），缺失则拦截并列出；并对材料名与上传文件名做相符性弱校验')
B('第三层 办结前：出证岗必须核对申报材料清单并勾选确认，把"人工把关"从隐式变成显式留痕')
WARN('如实说明的局限：程序无法判断"这张图是不是真的健康证"——那需要 OCR 识别内容或人工核对。当前 OCR 服务（PaddleOCR）依赖较重未启用，因此内容真实性判断依赖人工，这也是政务审批必须保留人工环节的原因。')

H2('Q7：为什么要"规则 + AI 分工"，而不是让 AI 直接判断？')
A('像判卷子：客观题交给机器（规则引擎），主观题请专家看（大模型），但最后签字的必须是老师（人工决定）。')
B('规则引擎：标准明确、算得快、结果可解释，能说清卡在哪一条，且可复现、可审计')
B('大模型：需要理解与判断，负责给核查要点与风险提示')
B('人工：审批是要负法律责任的行政决定，必须有人负责；规则全通过也只是"快进到办结岗等待复核"，不自动办结')
B('留痕：审计日志中 AI 建议与人工决定分开记录，出了问题能说清责任')
P('这是我们整个项目最有价值的设计，也是政务系统使用 AI 的正确方式——帮人干活，但不替人担责。')

H2('Q8：跨端数据一致性怎么保证？')
B('单库内：使用事务，保证审批实例状态与任务记录要么一起成功、要么一起回滚')
B('跨端回调：办结后回调企业端（出证）与群众端（通知），失败不阻断主流程，只记录日志，并全程审计留痕')
B('幂等：回调接口设计为幂等，重复通知也只处理一次，避免"一次办结被处理两遍"')
B('并发：审批任务办理采用 CAS 条件更新（只有任务仍是"待处理"才能更新成功），防止两个人同时办理同一件')
B('消息：审批推监管走消息队列（队列持久化），保证消息不丢')

# ==================== 第二部分：合并逐页讲稿 ====================
doc.add_page_break()
H1('第二部分  27 页结题答辩 PPT · 逐页讲稿')

if os.path.exists(SRC_SPEECH):
    src = Document(SRC_SPEECH)
    for element in src.element.body:
        if element.tag.endswith('}sectPr'):
            continue
        doc.element.body.append(element)
    print('已合并逐页讲稿:', SRC_SPEECH)
else:
    P('（未找到逐页讲稿文件，请先运行 gen_speech_27.py 生成）')

doc.save(OUT)
print('已生成汇总：', OUT)
