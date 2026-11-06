# -*- coding: utf-8 -*-
"""在第 13 页监管端补充"差评整改闭环"能力"""
import copy
from pptx import Presentation

SRC = r'D:\Desktop\企业实训-智能政务系统\文档\GovAI One 智慧政务系统 · 结题答辩.pptx'
p = Presentation(SRC)
s13 = p.slides[12]

target = None
for sh in s13.shapes:
    if sh.has_text_frame and '双随机抽查' in sh.text_frame.text:
        target = sh
        break

if target is None:
    print('未找到目标文本框')
else:
    tf = target.text_frame
    last = tf.paragraphs[-1]._p
    new_p = copy.deepcopy(last)
    last.addnext(new_p)
    para = tf.paragraphs[-1]
    text = '差评整改：群众 / 企业差评（≤2 星）自动上报生成工单，监管限期整改闭环'
    if para.runs:
        para.runs[0].text = text
        for r in para.runs[1:]:
            r.text = ''
    else:
        para.text = text
    p.save(SRC)
    print('已补充第 13 页「差评整改」一行')
    print('当前该文本框内容：')
    for i, x in enumerate(target.text_frame.paragraphs):
        print(f'  {i}. {x.text}')
