# -*- coding: utf-8 -*-
"""生成团队贡献度表（多 Sheet 单文件）"""
from openpyxl import Workbook
from openpyxl.styles import Font, PatternFill, Alignment, Border, Side
from openpyxl.utils import get_column_letter

HEAD_FILL = PatternFill('solid', fgColor='2F6BFF')
HEAD_FONT = Font(name='微软雅黑', size=11, bold=True, color='FFFFFF')
SUB_FILL = PatternFill('solid', fgColor='EAF0FF')
BODY_FONT = Font(name='微软雅黑', size=10)
BOLD = Font(name='微软雅黑', size=10, bold=True)
TITLE_FONT = Font(name='微软雅黑', size=14, bold=True, color='1F2D3D')
NOTE_FONT = Font(name='微软雅黑', size=9, color='909399')
THIN = Side(style='thin', color='D0D5DD')
BORDER = Border(left=THIN, right=THIN, top=THIN, bottom=THIN)
CENTER = Alignment(horizontal='center', vertical='center', wrap_text=True)
LEFT = Alignment(horizontal='left', vertical='center', wrap_text=True)

wb = Workbook()

# ============ Sheet1 贡献度总览 ============
ws = wb.active
ws.title = '团队贡献度总览'
ws['A1'] = 'GovAI One 智慧政务系统 · 团队贡献度总览'
ws['A1'].font = TITLE_FONT
ws.merge_cells('A1:L1')
ws['A2'] = '数据来源：《12组-GovAI-One-智慧政务系统需求矩阵》、四库实际表数、代码仓库统计（详见「统计说明」）'
ws['A2'].font = NOTE_FONT
ws.merge_cells('A2:L2')

headers = ['成员', '负责端', '需求数(条)', '需求占比', '后端代码(行)', '前端代码(行)',
           '代码合计(行)', '代码占比', '数据库表(张)', '前后端端口', '技术特色', '核心职责']
rows = [
    ['张梦凡', '群众端', 24, '28.9%', 3326, 5213, 8539, '21.0%', 9, '8083 / 5174', 'uni-app 跨端',
     '手机号登录、事项浏览与检索、在线申报、进度查询、AI 助手多轮对话、消息中心、好差评、关怀版（适老化）'],
    ['张可', '审批端', 19, '22.9%', 3783, 3587, 7370, '18.1%', 9, '8082 / 5173', '流程引擎 + 规则引擎 + LangChain4j + RabbitMQ',
     '流程模板与可视化设计器、规则引擎与规则编辑器、AI 审核意见、多岗位流转、CAS 防重复办理、审计日志、MQ 推送监管'],
    ['郝锦瑞', '企业端', 19, '22.9%', 7264, 7472, 14736, '36.2%', 17, '8084 / 5175', 'OCR + 电子证照 + 合规预检',
     '办事大厅、智能办事规划、申报与提交前预检、免证办、证照空间（全生命周期）、政策匹配、信用门禁、一件事套餐、经办人管理'],
    ['杨星宇', '监管端', 21, '25.3%', 6480, 3634, 10114, '24.8%', 16, '8081 / 5176', 'ECharts + WebSocket + SSO 认证中心',
     '数据大屏（实时推送）、MQ 数据汇聚、双随机抽查、风险预警与处置、信用体系与信用修复、审计追溯、RBAC 权限、差评整改闭环'],
    ['合计', '—', 83, '100%', 20853, 19906, 40759, '100%', 51, '—', '—', '四人垂直切片，各自独立完成「前端 + 后端 + 数据库」全链路'],
]

start = 4
for c, h in enumerate(headers, 1):
    cell = ws.cell(row=start, column=c, value=h)
    cell.fill = HEAD_FILL; cell.font = HEAD_FONT; cell.alignment = CENTER; cell.border = BORDER

for i, r in enumerate(rows):
    for c, v in enumerate(r, 1):
        cell = ws.cell(row=start + 1 + i, column=c, value=v)
        cell.font = BOLD if r[0] == '合计' else BODY_FONT
        cell.border = BORDER
        if r[0] == '合计':
            cell.fill = SUB_FILL
        cell.alignment = LEFT if c in (11, 12) else CENTER

widths = [10, 10, 11, 10, 13, 13, 13, 10, 13, 14, 30, 62]
for i, w in enumerate(widths, 1):
    ws.column_dimensions[get_column_letter(i)].width = w
ws.row_dimensions[start].height = 32
for r in range(start + 1, start + 1 + len(rows)):
    ws.row_dimensions[r].height = 56

# ============ Sheet2 需求分布 ============
ws2 = wb.create_sheet('需求分布')
ws2['A1'] = '需求分布（83 条 · 编号即契约 · 一人负责一端）'
ws2['A1'].font = TITLE_FONT
ws2.merge_cells('A1:E1')
ws2['A2'] = '来源：《12组-GovAI-One-智慧政务系统需求矩阵.xlsx》—— 逐条标注编号 / 大类 / 说明 / 负责人 / 状态'
ws2['A2'].font = NOTE_FONT
ws2.merge_cells('A2:E2')

h2 = ['端', '需求条数', '编号区间', '负责人', '占比']
r2 = [
    ['群众端', 24, 'NO.1 – 24', '张梦凡', '28.9%'],
    ['审批端', 19, 'NO.25 – 43', '张可', '22.9%'],
    ['企业端', 19, 'NO.44 – 62', '郝锦瑞', '22.9%'],
    ['监管端', 21, 'NO.63 – 83', '杨星宇', '25.3%'],
    ['合计', 83, 'NO.1 – 83', '—', '100%'],
]
s2 = 4
for c, h in enumerate(h2, 1):
    cell = ws2.cell(row=s2, column=c, value=h)
    cell.fill = HEAD_FILL; cell.font = HEAD_FONT; cell.alignment = CENTER; cell.border = BORDER
for i, r in enumerate(r2):
    for c, v in enumerate(r, 1):
        cell = ws2.cell(row=s2 + 1 + i, column=c, value=v)
        cell.font = BOLD if r[0] == '合计' else BODY_FONT
        cell.border = BORDER; cell.alignment = CENTER
        if r[0] == '合计':
            cell.fill = SUB_FILL
for i, w in enumerate([12, 12, 16, 12, 10], 1):
    ws2.column_dimensions[get_column_letter(i)].width = w

# ============ Sheet3 统计说明 ============
ws3 = wb.create_sheet('统计说明')
ws3['A1'] = '统计口径与说明（可复核）'
ws3['A1'].font = TITLE_FONT
ws3.merge_cells('A1:B1')
notes = [
    ('统计时间', '2026-09-21'),
    ('需求数', '来自《12组-GovAI-One-智慧政务系统需求矩阵.xlsx》，四端合计 83 条（群众端 24 / 审批端 19 / 企业端 19 / 监管端 21），编号 NO.1–83'),
    ('后端代码量', '统计各端 src/main/java 下全部 .java 文件的行数（含注释与空行），使用 wc -l 汇总'),
    ('前端代码量', '统计各端 src 下全部 .vue / .ts / .js 文件的行数（不含 node_modules 与打包产物 dist）'),
    ('数据库表数', '来自四库 information_schema 实测：govai_citizen 9 · govai_approval 9 · govai_enterprise 17 · govai_supervision 16，合计 51'),
    ('需求占比', '按各端需求条数 / 83 计算（需求编号即契约，是最客观的分工依据）'),
    ('代码占比', '按各端（后端 + 前端）代码行数 / 40759 计算'),
    ('关于综合贡献度', '本表不直接给出单一百分比，而是并列「需求数」「代码量」「数据库表」三个客观维度——四端职责难度不同（如审批端含规则引擎与大模型集成、企业端表单与证照逻辑最重），单一百分比容易失真；如需一个数值，建议以「需求数占比」为准'),
    ('团队协作方式', '垂直切片：一人负责一个端的完整闭环（前端 + 后端 + 数据库），通过 10 个接口契约协作，不交叉改代码'),
]
r = 3
for k, v in notes:
    ws3.cell(row=r, column=1, value=k).font = BOLD
    ws3.cell(row=r, column=1).alignment = LEFT
    c2 = ws3.cell(row=r, column=2, value=v)
    c2.font = BODY_FONT; c2.alignment = LEFT
    ws3.row_dimensions[r].height = 42
    r += 1
ws3.column_dimensions['A'].width = 16
ws3.column_dimensions['B'].width = 95

out = r'D:\Desktop\企业实训-智能政务系统\文档\GovAI-One-团队贡献度表.xlsx'
wb.save(out)
print('已生成：', out)
