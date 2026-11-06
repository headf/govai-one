# -*- coding: utf-8 -*-
"""生成《成果物整理方案与交付清单》（命令用单行 raw 字符串，避免引号嵌套问题）"""
from docx import Document
from docx.shared import Pt, RGBColor
from docx.oxml.ns import qn
from docx.oxml import OxmlElement

FONT = '微软雅黑'
MONO = 'Consolas'
INK = RGBColor(0x1F, 0x2D, 0x3D)
BLUE = RGBColor(0x2F, 0x6B, 0xFF)
GRAY = RGBColor(0x8A, 0x8A, 0x8A)
RED = RGBColor(0xC0, 0x39, 0x2B)

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


def _f(run, size=10.5, bold=False, color=INK, font=FONT, italic=False):
    run.font.name = font
    run._element.get_or_add_rPr()
    run._element.rPr.rFonts.set(qn('w:eastAsia'), font)
    run.font.size = Pt(size)
    run.bold = bold
    run.italic = italic
    if color is not None:
        run.font.color.rgb = color


def title(t):
    p = doc.add_paragraph(); p.alignment = 1
    _f(p.add_run(t), 20, True)


def sub(t):
    p = doc.add_paragraph(); p.alignment = 1
    _f(p.add_run(t), 10, color=GRAY)
    p.paragraph_format.space_after = Pt(12)


def H1(t):
    p = doc.add_paragraph()
    _f(p.add_run(t), 15, True, BLUE)
    p.paragraph_format.space_before = Pt(16); p.paragraph_format.space_after = Pt(6)


def H2(t):
    p = doc.add_paragraph()
    _f(p.add_run(t), 12, True)
    p.paragraph_format.space_before = Pt(10); p.paragraph_format.space_after = Pt(3)


def P(t):
    p = doc.add_paragraph(); _f(p.add_run(t))
    return p


def B(t):
    p = doc.add_paragraph(); p.paragraph_format.left_indent = Pt(16)
    _f(p.add_run('· ' + t))
    return p


def CODEM(*lines):
    for line in lines:
        p = doc.add_paragraph()
        p.paragraph_format.left_indent = Pt(16)
        p.paragraph_format.space_after = Pt(0)
        p.paragraph_format.line_spacing = 1.15
        _f(p.add_run(line), 9.5, font=MONO, color=RGBColor(0x24, 0x29, 0x2E))


def NOTE(t):
    p = doc.add_paragraph(); p.paragraph_format.left_indent = Pt(12)
    _f(p.add_run(t), 9.5, color=GRAY, italic=True)


def WARN(t):
    p = doc.add_paragraph(); p.paragraph_format.left_indent = Pt(12)
    _f(p.add_run('⚠ ' + t), 9.5, True, RED)


def table(headers, rows):
    t = doc.add_table(rows=1, cols=len(headers))
    t.style = 'Light Grid Accent 1'
    for i, h in enumerate(headers):
        c = t.rows[0].cells[i]; c.text = ''
        _f(c.paragraphs[0].add_run(h), 10, True)
    for r in rows:
        cells = t.add_row().cells
        for i, v in enumerate(r):
            cells[i].text = ''
            _f(cells[i].paragraphs[0].add_run(str(v)), 9.5)
    return t


# ==================== 封面 ====================
title('GovAI One 智慧政务系统')
sub('成果物整理方案 与 交付清单')
NOTE('说明：本文件只给方案，不移动任何文件。命令可复制到 CMD 执行（Windows）。')

# ==================== 一、现状 ====================
H1('一、当前文件盘点（实测）')
P('项目根目录散落了 25 个与代码无关的文件；同时已有 4 个成体系的归档目录。')

H2('1.1 已有整理结构（保持不动）')
B('backups/ —— 9 个 SQL 备份（命名规范：xxx_before_xxx_日期.sql）')
B('logs/ —— 运行日志归档（含 from-* 子目录）')
B('archive/ —— 旧版本与提取产物（内含 1 个 erl_crash.dump）')
B('文档/ —— 已分「需求 / 设计 / 原型图 / 讲稿-分人」')

H2('1.2 根目录散落文件（需要归位）')
table(['类别', '数量', '文件', '建议去处'], [
    ['运行日志', 9, 'logs-approval-backend.txt 等', 'logs/'],
    ['工具脚本', 9, 'gen_*.py（6）、fix_*.py（3）', 'scripts/'],
    ['运维脚本', 3, '开放局域网端口.bat 等', 'scripts/运维/'],
    ['SQL 备份', 1, 'backup_license_before_cleanup_20260921.sql', 'backups/'],
    ['重复压缩包', 3, '文档.zip / 文档(1).zip / 文档(2).zip', '删除（先确认内容已在 文档/）'],
    ['崩溃转储', 1, 'erl_crash.dump', '删除（Erlang 转储，无用途）'],
])

# ==================== 二、目标结构 ====================
H1('二、推荐的目标结构')
P('原则：代码目录保持原位（理由见第五节），只整理非代码文件，用 README 做统一导航。')
CODEM('企业实训-智能政务系统/',
      '├── README.md                 ← 新建：项目简介 + 快速启动 + 目录导航',
      '├── 交付物清单.md              ← 新建：答辩成果物索引',
      '│',
      '├── GovAi/govai-enterprise/   企业端（代码，原位不动）',
      '├── govai-approval/           审批端（代码，原位不动）',
      '├── Supervision/              监管端（代码，原位不动）',
      '├── 智慧政务系统-群众端/        群众端（代码，原位不动）',
      '│',
      '├── 文档/                     已有，继续完善',
      '│   ├── 需求/                 需求矩阵、规约、分析',
      '│   ├── 设计/                 骨架文档、分工方案、模块图、设计令牌',
      '│   ├── 答辩/                 【新建】答辩交付物集中处',
      '│   ├── 原型/                 原型图.rar',
      '│   └── 过程稿/               【新建】通俗讲稿、PPT 提纲等中间产物',
      '│',
      '├── scripts/                  【新建】工具与运维脚本',
      '│   ├── 生成文档/             gen_*.py',
      '│   ├── 修复PPT/              fix_*.py',
      '│   └── 运维/                 启动脚本、开放局域网端口.bat',
      '│',
      '├── backups/                  已有：SQL 备份',
      '├── logs/                     已有：运行日志',
      '└── archive/                  已有：旧版本、提取产物')

# ==================== 三、动作清单 ====================
H1('三、逐项整理动作（含可复制命令）')
P('在 CMD 中于项目根目录依次执行；每步独立，可跳过。')

H2('步骤 1：建立新目录')
CODEM(r'cd /d "D:\Desktop\企业实训-智能政务系统"',
      r'mkdir "文档\答辩" 2>nul',
      r'mkdir "文档\过程稿" 2>nul',
      r'mkdir "scripts\生成文档" 2>nul',
      r'mkdir "scripts\修复PPT" 2>nul',
      r'mkdir "scripts\运维" 2>nul')

H2('步骤 2：答辩交付物 → 文档\\答辩\\')
CODEM(r'move "文档\GovAI One 智慧政务系统 · 结题答辩.pptx" "文档\答辩\"',
      r'move "文档\GovAI-One-逐页讲稿(27页结题答辩).docx" "文档\答辩\"',
      r'move "文档\GovAI-One-术语解释手册.docx" "文档\答辩\"',
      r'move "文档\GovAI-One-功能实现与技术原理.docx" "文档\答辩\"',
      r'move "文档\GovAI-One-团队贡献度表.xlsx" "文档\答辩\"',
      r'move "文档\讲稿-分人" "文档\答辩\"')

H2('步骤 3：过程稿 → 文档\\过程稿\\')
CODEM(r'move "文档\GovAI-One-答辩讲稿(通俗版).docx" "文档\过程稿\"',
      r'move "文档\GovAI-One-答辩PPT提纲.pptx" "文档\过程稿\"',
      r'move "文档\GovAI-One-逐页讲稿(对照PPT).docx" "文档\过程稿\"')

H2('步骤 4：工具脚本 → scripts\\')
CODEM(r'move gen_*.py "scripts\生成文档\"',
      r'move fix_*.py "scripts\修复PPT\"',
      r'move "开放局域网端口.bat" "scripts\运维\"')

H2('步骤 5：备份与归档归位')
CODEM(r'move backup_license_before_cleanup_20260921.sql backups\ ',
      r'move erl_crash.dump archive\ ')

H2('步骤 6：清理重复与无用文件（执行前请确认）')
CODEM(r'del "文档.zip"',
      r'del "文档(1).zip"',
      r'del "文档(2).zip"',
      r'del "文档\讲稿-分人.zip"')
WARN('删除前请先解压确认：这三个压缩包的内容已在 文档/ 中，属于早期打包副本。')

H2('步骤 7：日志归档（需先停服务）')
WARN('根目录 9 个 logs-*.txt 正被运行中的服务写入，Windows 会拒绝移动。')
B('做法 A（推荐，不中断）：先改启动脚本，把日志输出路径统一指向 logs/，下次重启自然生效')
B('做法 B（立即生效）：停四端服务 → 移动日志 → 重新启动（中断约 2 分钟）')
CODEM(r':: 做法 B（停服务后执行）',
      r'move logs-*.txt logs\ ')

# ==================== 四、交付清单 ====================
doc.add_page_break()
H1('四、答辩交付物清单（成果物总表）')

H2('4.1 代码成果（4 端 8 个工程）')
table(['端', '后端工程', '前端工程', '后端代码', '前端代码', '端口'], [
    ['群众端', 'govai-citizen-server', 'govai-citizen-ui(-hx)', '3,326 行', '5,213 行', '8083 / 5174'],
    ['审批端', 'govai-approval-server', 'govai-approval-ui', '3,783 行', '3,587 行', '8082 / 5173'],
    ['企业端', 'govai-enterprise-server', 'govai-enterprise-ui', '7,264 行', '7,472 行', '8084 / 5175'],
    ['监管端', 'Supervision', 'govai-supervision-ui', '6,480 行', '3,634 行', '8081 / 5176'],
    ['合计', '—', '—', '20,853 行', '19,906 行', '代码共约 40,759 行'],
])

H2('4.2 数据库成果')
table(['Schema', '表数', '主要表'], [
    ['govai_citizen', 9, '群众用户 / 事项 / 申报 / 材料 / 通知 / 消息 / 评价 / 政策推荐'],
    ['govai_approval', 9, '流程模板 / 节点 / 实例 / 任务 / 规则集 / 规则项 / 审计 / Prompt / AI 对话'],
    ['govai_enterprise', 17, '企业用户 / 企业信息 / 法人事项 / 申报 / 材料 / 电子证照 / 合规预检 / 政策匹配 / 套餐 / 问答 / 经办人 / 补贴'],
    ['govai_supervision', 16, '用户 / 角色 / 权限 / 信用 / 预警 / 抽查 / 差评工单 / 审计追溯 / 日统计 / 审批缓存 / 字典 / 日志'],
    ['合计', 51, '四库逻辑隔离，跨端只走接口'],
])

H2('4.3 文档成果')
table(['类别', '文件', '说明'], [
    ['需求', '需求矩阵.xlsx（83 条）', '编号即契约，逐条对应负责人'],
    ['需求', '需求规约.docx / 需求分析.docx', '依据 GB/T 9385-2008'],
    ['设计', '全组开发骨架文档.docx', '技术栈 / 数据库 / API 约定 / 项目结构'],
    ['设计', '团队分工与技术栈方案.docx / 按角色分工方案.docx', '四人垂直切片分工'],
    ['设计', '功能模块图.docx / 群众端项目大纲.pptx', '模块与职责划分'],
    ['答辩', '结题答辩.pptx（27 页）', '已按系统实际修正，含演示第 ⑧ 步'],
    ['答辩', '逐页讲稿(27页).docx', '逐页照念稿 + 英文术语注解 + 技术说明'],
    ['答辩', '讲稿-分人/（4 份）', '每位成员专属讲稿'],
    ['答辩', '术语解释手册.docx', '69 个术语白话解释 + 答辩话术'],
    ['答辩', '功能实现与技术原理.docx', '54 个功能：怎么实现 / 用什么技术 / 技术是什么'],
    ['答辩', '团队贡献度表.xlsx', '需求数 / 代码量 / 表数 三维度'],
    ['原型', '原型图.rar', '界面原型'],
])

H2('4.4 演示资源')
table(['资源', '内容'], [
    ['演示账号', '企业端 ent_admin/123456；审批端 approver1（或 decide1 / issue1）/123456；监管端 supervisor1/123456；群众端 手机号 13800000020 + 验证码 000000'],
    ['演示脚本', 'PPT 第 25 页（8 步）+ 逐页讲稿第 25 页（含演示前检查清单）'],
    ['实时数据', '监管大屏数据由审批端经 RabbitMQ 推送产生（非静态假数据）'],
    ['差评闭环', '提 1 星差评 → 监管端「监管业务 → 差评整改」出现工单，可现场处置'],
])

H2('4.5 运维与启动')
table(['脚本 / 资源', '用途'], [
    ['各端 start-backend.bat / 启动脚本', '启动后端服务'],
    ['开放局域网端口.bat', '放行 5173-5177，供局域网访问'],
    ['数据库脚本', '企业端 sql/ 13 个、监管端 sql/ 7 个、群众端 sql/ 2 个（建表 + 种子 + 增量升级）'],
])

# ==================== 五、风险 ====================
H1('五、风险提示与取舍说明')

H2('5.1 为什么建议代码目录不动')
B('四端是独立工程：各有 .idea / pom.xml / node_modules / 启动脚本，路径互相依赖')
B('移动后需同步修改：IDE 工程配置、启动脚本相对路径、前后端代理配置')
B('收益（目录更整齐）明显低于风险（服务起不来）')
B('替代方案：用 README.md 做统一导航，效果接近且零风险')

H2('5.2 执行前必须确认的三件事')
WARN('删除 3 个「文档*.zip」前，请确认内容已在 文档/ 中')
WARN('移动 logs-*.txt 前，先停止四端服务，否则提示文件被占用')
WARN('移动答辩 PPT 前，先关闭 PowerPoint，否则文件被锁无法移动')

H2('5.3 可选增强（按需）')
B('在 .gitignore 中排除 logs/、backups/、node_modules/、target/，避免仓库臃肿')
B('把四端数据库脚本复制一份到 文档/数据库/，便于答辩展示"建库脚本"')
B('README.md 中配一张系统架构图（可直接截 PPT 第 4 页或第 9 页）')

# ==================== 六、执行顺序 ====================
H1('六、建议的执行顺序（可分批）')
table(['批次', '内容', '耗时', '风险'], [
    ['第 1 批', '建目录 + 答辩交付物集中（步骤 1、2）', '2 分钟', '无'],
    ['第 2 批', '工具脚本与备份归位（步骤 3、4、5）', '2 分钟', '无'],
    ['第 3 批', '清理重复文件（步骤 6）', '1 分钟', '需确认后执行'],
    ['第 4 批', '日志归档（步骤 7）', '5 分钟', '需停服务'],
    ['第 5 批', '写 README.md + 交付物清单.md', '—', '无'],
])

out = r'D:\Desktop\企业实训-智能政务系统\文档\GovAI-One-成果物整理方案与交付清单.docx'
doc.save(out)
print('已生成：', out)
