# -*- coding: utf-8 -*-
"""生成《GovAI One 智慧政务系统 · 答辩讲稿（通俗版）》Word 文档"""
from docx import Document
from docx.shared import Pt, RGBColor
from docx.oxml.ns import qn
from docx.oxml import OxmlElement

FONT = '微软雅黑'
INK = RGBColor(0x1F, 0x2D, 0x3D)
BLUE = RGBColor(0x2F, 0x6B, 0xFF)
GRAY = RGBColor(0x8A, 0x8A, 0x8A)

doc = Document()
# 页面默认字体
st = doc.styles['Normal']
st.font.name = FONT
st.font.size = Pt(11)
rpr = st.element.get_or_add_rPr()
rf = rpr.find(qn('w:rFonts'))
if rf is None:
    rf = OxmlElement('w:rFonts')
    rpr.append(rf)
rf.set(qn('w:eastAsia'), FONT)
st.paragraph_format.line_spacing = 1.5
st.paragraph_format.space_after = Pt(4)


def _set(run, size=11, bold=False, color=None, italic=False):
    run.font.name = FONT
    run._element.get_or_add_rPr()
    run._element.rPr.rFonts.set(qn('w:eastAsia'), FONT)
    run.font.size = Pt(size)
    run.bold = bold
    run.italic = italic
    if color is not None:
        run.font.color.rgb = color


def title(text):
    p = doc.add_paragraph()
    p.alignment = 1
    _set(p.add_run(text), size=22, bold=True, color=INK)
    p.paragraph_format.space_after = Pt(6)


def subtitle(text):
    p = doc.add_paragraph()
    p.alignment = 1
    _set(p.add_run(text), size=11, color=GRAY)
    p.paragraph_format.space_after = Pt(16)


def h1(text):
    p = doc.add_paragraph()
    _set(p.add_run(text), size=16, bold=True, color=BLUE)
    p.paragraph_format.space_before = Pt(16)
    p.paragraph_format.space_after = Pt(8)


def h2(text):
    p = doc.add_paragraph()
    _set(p.add_run(text), size=13, bold=True, color=INK)
    p.paragraph_format.space_before = Pt(10)
    p.paragraph_format.space_after = Pt(4)


def t(text, bold=False):
    p = doc.add_paragraph()
    _set(p.add_run(text), bold=bold)
    return p


def bullet(text):
    p = doc.add_paragraph()
    p.paragraph_format.left_indent = Pt(16)
    _set(p.add_run('· ' + text))
    return p


def quote(text):
    """框注/提醒样式"""
    p = doc.add_paragraph()
    p.paragraph_format.left_indent = Pt(12)
    _set(p.add_run(text), size=10, color=GRAY, italic=True)
    return p


# ============================== 正文 ==============================
title('GovAI One 智慧政务系统')
subtitle('答辩讲稿（通俗版）—— 可以直接照着念')

t('说明：这份讲稿是"大白话版"，尽量不用专业术语；括号里的内容是给你的提示，念的时候不用说。')
t('全文念下来约 8～10 分钟。')
doc.add_paragraph()

h1('一、开场白')
t('各位老师好，我是软件工程专业的【这里填你的名字】。我们小组做的是一个智慧政务系统，名字叫 GovAI One。')
t('下面我尽量用大白话，把我们这个系统讲清楚。')

h1('二、这个系统到底是干什么的')
t('先打个比方。现实中的政务大厅是这样的：老百姓来办社保、企业来办营业执照、窗口的工作人员负责审核材料、后面还有监管部门检查有没有违规。')
t('我们这个系统，就是把这个政务大厅搬到网上，而且请了一位 AI 当助手。')
t('我们一共做了四个"窗口"，分别给四种人用：')
bullet('普通老百姓：用微信小程序办事')
bullet('企业：用电脑网页办事')
bullet('政府工作人员：用电脑网页审核')
bullet('监管部门：用大屏和后台看数据、查问题')
t('这四个窗口不是各干各的，而是串成了一条完整的流水线：')
t('老百姓或企业提交申请 → 系统自动检查材料 → 工作人员审核 → 办完自动发证照 → 数据自动送到监管大屏。', bold=True)

h1('三、四个端分别能做什么')

h2('1. 群众端（微信小程序）—— 给普通老百姓用')
t('老百姓最常办的是社保、医保、户口这类个人事项。我们做了这些功能：')
bullet('手机号加验证码登录')
bullet('浏览事项、在线填表提交')
bullet('实时查看办理进度')
bullet('AI 助手答疑：比如直接问"我要迁户口需要什么材料"，它会回答')
bullet('办完之后，手机上自动收到消息通知')
t('我们还专门做了一个"关怀版"——字更大、按钮更大，方便老年人使用。')

h2('2. 企业端（电脑网页）—— 给企业办事员用')
t('企业办的证比较多：营业执照、食品经营许可证、卫生许可证等等。我们最大的亮点是让企业"少填表"：')
bullet('企业信息自动带出来，不用重复填写')
bullet('上传营业执照照片，系统用 OCR 自动识别文字，帮企业把表格填好')
bullet('公司已经有电子证照的，系统自动核验，材料都不用再传（我们叫它"免证办"）')
bullet('提交之前系统先自动检查一遍，缺什么、错什么，当场告诉你，可以返回修改')
t('除了办事，还有几个实用的功能：')
bullet('证照空间：像卡包一样管理公司的所有证照，快过期了会提醒')
bullet('政策推荐：系统自动匹配你能享受的补贴政策，一键确认')
bullet('智能规划：你用一句话说"我想开一家餐馆"，系统帮你列出需要办哪些事')
bullet('还有好差评、信用监测、经办人管理')

h2('3. 审批端（电脑网页）—— 给政府审批员用')
t('这是技术最复杂的一端。工作人员每天要处理很多申请，我们做了：')
bullet('待办列表：按岗位分工——受理初审、终审决定、办结出证，各看各的待办')
bullet('自动审核：系统按提前配置好的规则自动检查（比如"经营面积必须大于 5 平方米""必须有有效的营业执照"）。如果规则全部通过，就直接流转到办结岗')
bullet('AI 审核意见：AI 会给出一段"核查要点和风险提示"，比如"经营地址只写到了学校，建议核实具体位置"，帮工作人员快速判断')
bullet('审批操作：通过、驳回、退回补正')
bullet('流程配置和规则配置：管理员可以自己配流程、配规则，不用改代码')

h2('4. 监管端（大屏 + 后台）—— 给监管部门用')
bullet('数据大屏：实时显示办件量、办理时长、热点事项等，数据是从审批端自动推过来的')
bullet('双随机抽查：抽查任务的抽取和记录')
bullet('风险预警：配置预警规则，发现问题自动告警')
bullet('信用管理：给企业打信用分（A、B、C、D 四个等级），信用差的会被限制，比如 D 级企业不能申请补贴；还支持"信用修复"')
bullet('系统管理：用户、角色、权限、操作日志')

h1('四、我们用了哪些技术，为什么用')
t('先说明一下：后端四个端用的技术是一样的；前端分两类——三个电脑端一样，手机端单独一套。')

h2('后端（服务端）')
bullet('Spring Boot：Java 最主流的开发框架。就像盖房子用的"标准框架"，很多东西不用自己造。')
bullet('MySQL 数据库：存所有数据。我们四个端各用一个独立的库，互相不能直接看对方的数据，只能通过接口要——这是政务数据的安全要求。')
bullet('MyBatis-Plus：帮我们少写很多重复的数据库代码。')
bullet('OpenFeign：让一个端能"打电话"给另一个端。比如企业端提交申请时，直接调用审批端的接口，写代码的时候就像调用本地方法一样简单。')
bullet('RabbitMQ（消息队列）：审批端办完一件事，要把数据"寄"给监管端。用消息队列就像一个邮局——寄出去就不用管了，审批员不用等监管端收完才能下班。')
bullet('JWT：一种"电子通行证"。登录一次，四个端之间互相认可，不用反复登录。')
bullet('Redis：临时数据的高速缓存。')
bullet('大模型（AI）：通过 LangChain4j 接入了 DeepSeek 大模型。用它的原因是它把"调用 AI"这一步标准化了——以后想换别的 AI 模型，只改一行配置就行。')

h2('前端（界面）')
bullet('Vue 3 + Element Plus：做电脑网页最常用的组合。Element Plus 提供了现成的表格、表单、按钮，我们不用从零画。')
bullet('uni-app：手机端用它。好处是写一套代码，能同时变成微信小程序和手机网页，不用写两遍。')
bullet('ECharts：监管大屏上的图表用它画。')
bullet('Tailwind CSS：我们用它统一了四个端的外观（颜色、字号、圆角），保证风格一致。')

h1('五、我们最想讲的一个设计：让 AI 帮忙，但不让 AI 拍板')
t('这一点我们认为是整个项目里最有价值的设计。打个比方，就像判卷子：')
bullet('客观题交给机器（规则引擎）：比如"面积够不够大""证照有没有过期"，这种有明确标准的，机器判断又快又准，而且结果能解释——为什么没过、卡在哪一条，都能说清楚。')
bullet('主观题请专家看（大模型）：比如"这份材料有没有需要进一步核实的风险点"，这种需要理解和判断的，交给 AI。')
bullet('但最后签字的还是老师（人工决定）：因为审批是要负法律责任的行政决定，必须有人负责。')
t('所以在我们的系统里，"通过还是不通过"是规则算出来的，AI 只负责写"注意事项"，最后一定要人工确认。我们认为，这是政务系统使用 AI 的正确方式。', bold=True)

h1('六、我们的分工')
t('我们四个人，一人负责一个端，从界面到数据库全部自己写：')
bullet('张梦凡：群众端（微信小程序 + H5）')
bullet('张可：审批端（审批工作台）')
bullet('郝锦瑞：企业端（企业服务门户）')
bullet('杨星宇：监管端（监管大屏 + 后台）')
quote('提示：如果你是其中某位同学，讲稿里只保留自己那一段详细讲即可；其他人一句话带过。')

h1('七、现场演示流程（照着走一遍）')
t('1. 打开企业端，用一个企业账号登录。')
t('2. 在办事大厅里选"食品经营许可新办"，把表格填好并提交。')
t('3. 打开审批端，用审批员账号登录，在待办列表里找到刚才这一件。')
t('4. 点开详情，可以看到系统的自动审核结果和 AI 给出的核查意见。')
t('5. 点"通过"，或者点"办结"出证。')
t('6. 回到企业端刷新——证照空间里多了一张新的许可证。')
t('7. 打开监管端大屏——刚才这一单的数据已经实时显示出来了。')
t('8. 换到群众端（小程序），演示手机号登录、提交一个户口迁移申请。')

h1('八、结尾')
t('我们这个系统，核心做了两件事：')
t('第一，把政务办事的四个角色，用一条真实的数据链路串了起来，不是四个孤立的演示；')
t('第二，把 AI 放在了合适的位置上——帮人干活，但不替人担责。')
t('我的汇报完毕，谢谢各位老师。')

h1('九、附录：老师可能会问的问题，以及怎么回答')
quote('（这一页是给你自己看的，不用念出来。提前看两遍，被问到就不会慌。')

h2('Q1：你们这个 AI 用了向量数据库吗？')
t('答：目前没有用向量数据库。我们的做法是先从数据库里用关键词把相关的办理指南、政策内容找出来，再交给 AI 回答。这么做是因为我们的数据量不大、领域也很窄，关键词检索够用了。如果以后要处理大量文档，我们会引入向量数据库做语义检索。')
quote('重要提醒：绝对不要说用了 Neo4j、Milvus、知识图谱这些词——代码里没有实现，老师一追问细节就会答不上来。')

h2('Q2：AI 会不会随便给结论，或者乱说？')
t('答：不会。我们把"结论"和"建议"分开了：通过与不通过是规则算出来的，AI 只写注意事项。而且我们在给 AI 的要求里明确写了"只能引用表格里真实存在的信息，不许编造"。')

h2('Q3：为什么要做成四个系统，不做一个？')
t('答：因为这四种用户差别太大——老百姓用手机、企业用电脑、审批员用工作台、监管看大屏，权限也完全不同。拆开以后可以并行开发、各自独立部署，而且数据是隔离的，符合政务数据安全的要求。')

h2('Q4：审批推给监管，为什么用消息队列？')
t('答：因为这两个动作不需要"等"。审批员办完就该结束，不应该卡在那里等监管端收数据。用消息队列就像寄快递——交出去就行了。')

h2('Q5：如果两个人同时点"通过"，会怎样？')
t('答：我们做了防重复办理：更新任务的时候带了条件判断——只有任务还是"待处理"状态才能更新成功，第二个人的操作会失败，并提示"任务已被他人办理"。')

h2('Q6：四个端界面风格怎么保证一致？')
t('答：我们抽了一套共同的设计规范——主色、字号、圆角、状态颜色都统一定义好，电脑端用 Tailwind CSS 落地，手机端用同一套数值（单位换成 rpx），所以视觉上是统一的。')

h2('Q7：一个人负责一个端，忙得过来吗？')
t('答：确实不轻松，所以我们四个端用了同一套后端技术（Spring Boot），大家可以互相参考、遇到问题一起讨论；前端也统一了视觉规范，避免各写各的、最后拼不起来。')

out = r'D:\Desktop\企业实训-智能政务系统\文档\GovAI-One-答辩讲稿(通俗版).docx'
doc.save(out)
print('已生成：', out)
