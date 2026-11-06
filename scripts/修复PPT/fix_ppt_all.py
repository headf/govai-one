# -*- coding: utf-8 -*-
"""全 27 页核对后的补充修正"""
import shutil
from pptx import Presentation

SRC = r'D:\Desktop\企业实训-智能政务系统\文档\GovAI One 智慧政务系统 · 结题答辩.pptx'
BAK = r'D:\Desktop\企业实训-智能政务系统\文档\GovAI One 智慧政务系统 · 结题答辩(备份2).pptx'
shutil.copy(SRC, BAK)

p = Presentation(SRC)
slides = list(p.slides)
done = []


def set_cell(tbl, r, c, text):
    cell = tbl.cell(r, c)
    tf = cell.text_frame
    para = tf.paragraphs[0]
    if para.runs:
        para.runs[0].text = text
        for run in para.runs[1:]:
            run.text = ''
    else:
        para.text = text
    for extra in list(tf.paragraphs[1:]):
        extra._p.getparent().remove(extra._p)


def set_text(shape, text):
    tf = shape.text_frame
    para = tf.paragraphs[0]
    if para.runs:
        para.runs[0].text = text
        for run in para.runs[1:]:
            run.text = ''
    else:
        para.text = text
    for extra in list(tf.paragraphs[1:]):
        extra._p.getparent().remove(extra._p)


# ---------- 第 16 页：版本精确化 + 去掉"可重试" ----------
s16 = slides[15]
tbl = [sh for sh in s16.shapes if sh.has_table][0].table
set_cell(tbl, 2, 1, '3.5.7')
set_cell(tbl, 4, 2, '声明式同步调用；MQ 做异步解耦与削峰（队列持久化，保证消息不丢）')
set_cell(tbl, 7, 1, '7 / 5.8.26')
done.append('第16页: MyBatis-Plus 3.5.5+→3.5.7；MQ去掉"可重试"；Hutool→5.8.26')

# ---------- 第 25 页：演示流程加第 ⑧ 步 ----------
import copy
from pptx.oxml.ns import qn

s25 = slides[24]
tbl = [sh for sh in s25.shapes if sh.has_table][0].table
tbl_el = tbl._tbl
trs = tbl_el.findall(qn('a:tr'))
trs[-1].addnext(copy.deepcopy(trs[-1]))   # 复制最后一行插入到末尾
n = len(tbl.rows)
new_cells = tbl.rows[n - 1].cells
vals = ['⑧', '企业端给本次办事打 1 星差评', '监管端「监管业务 → 差评整改」立即出现工单，可填写整改措施闭环']
for i, v in enumerate(vals):
    tf = new_cells[i].text_frame
    para = tf.paragraphs[0]
    if para.runs:
        para.runs[0].text = v
        for run in para.runs[1:]:
            run.text = ''
    else:
        para.text = v
    for extra in list(tf.paragraphs[1:]):
        extra._p.getparent().remove(extra._p)
done.append('第25页: 演示流程新增第 ⑧ 步（差评 → 监管整改工单）')

# ---------- 第 26 页：9 个接口 → 10 个 ----------
s26 = slides[25]
for sh in s26.shapes:
    if sh.has_text_frame and '9 个接口协作' in sh.text_frame.text:
        set_text(sh, '每人独立完成前端 + 后端 + 数据库，通过 10 个接口协作，不交叉改代码')
        done.append('第26页: 9 个接口→10 个')
        break

# ---------- 第 27 页：总结数字 ----------
s27 = slides[26]
for sh in s27.shapes:
    if sh.has_text_frame and '9 个接口契约' in sh.text_frame.text:
        set_text(sh, '不是四个孤立的演示——10 个接口契约 · 51 张表 · 4 个独立服务 · 1 条 MQ 通道')
        done.append('第27页: 9 个接口·42 张表 → 10 个接口·51 张表')
        break

p.save(SRC)
print('已保存：', SRC)
print('备份：', BAK)
print()
for d in done:
    print(' -', d)
