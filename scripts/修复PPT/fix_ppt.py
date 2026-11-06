# -*- coding: utf-8 -*-
"""按系统实际内容修正 27 页结题答辩 PPT"""
import shutil
from pptx import Presentation

SRC = r'D:\Desktop\企业实训-智能政务系统\文档\GovAI One 智慧政务系统 · 结题答辩.pptx'
BAK = r'D:\Desktop\企业实训-智能政务系统\文档\GovAI One 智慧政务系统 · 结题答辩(修改前备份).pptx'
shutil.copy(SRC, BAK)

p = Presentation(SRC)
slides = list(p.slides)


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


def set_para(shape, idx, text):
    tf = shape.text_frame
    para = tf.paragraphs[idx]
    if para.runs:
        para.runs[0].text = text
        for run in para.runs[1:]:
            run.text = ''
    else:
        para.text = text


def set_text(shape, text):
    set_para(shape, 0, text)
    tf = shape.text_frame
    for extra in list(tf.paragraphs[1:]):
        extra._p.getparent().remove(extra._p)


def find(slide, pred):
    for sh in slide.shapes:
        if pred(sh):
            return sh
    return None


changes = []

# ---------- 第 8 页：表数 9/9/17/16，合计 51 ----------
s8 = slides[7]
tbl = find(s8, lambda x: x.has_table).table
set_cell(tbl, 3, 5, '17')   # 企业端
set_cell(tbl, 4, 5, '16')   # 监管端
for sh in s8.shapes:
    if sh.has_text_frame and sh.text_frame.text.strip().startswith('合计'):
        set_text(sh, '合计 51 张表 · 四库数据隔离')
        changes.append('第8页: 合计表数 42→51')
    if sh.has_text_frame and '个接口契约' in sh.text_frame.text:
        set_text(sh, '不是四个孤立演示 —— 10 个接口契约（Feign 同步）+ 1 条 MQ 通道（异步）串成完整业务链路')
        changes.append('第8页: 接口契约 9→10')

# ---------- 第 14 页：通知模板 → 提示词配置 ----------
s14 = slides[13]
for sh in s14.shapes:
    if not sh.has_text_frame:
        continue
    t = sh.text_frame.text.strip()
    if t == '通知模板':
        set_text(sh, '提示词配置')
        changes.append('第14页: 通知模板→提示词配置')
    elif t == '微信、短信通知模板统一管理':
        set_text(sh, 'AI 审核意见 / 问答的 Prompt 模板在线维护')
        changes.append('第14页: 通知模板描述→提示词描述')

# ---------- 第 15 页：Nacos → 固定地址直连 ----------
s15 = slides[14]
tbl = find(s15, lambda x: x.has_table).table
set_cell(tbl, 6, 0, '服务寻址')
set_cell(tbl, 6, 1, '固定地址直连（本地部署）；预留 Nacos 作为演进方向')
changes.append('第15页: Nacos 描述修正')

# ---------- 第 18 页：42→51 张表，明细修正 ----------
s18 = slides[17]
for sh in s18.shapes:
    if sh.has_text_frame and sh.text_frame.text.strip().startswith('数据库：'):
        set_text(sh, '数据库：51 张表，四库隔离')
        changes.append('第18页: 标题 42→51')
tbl = find(s18, lambda x: x.has_table).table
set_cell(tbl, 3, 2, '17')
set_cell(tbl, 3, 3, '企业用户 / 企业信息 / 法人事项 / 申报 / 材料 / 电子证照 / 合规预检 / 政策匹配 / 一件事套餐 / 问答会话 / 经办人 / 补贴发放')
set_cell(tbl, 4, 2, '16')
set_cell(tbl, 4, 3, '用户 / 角色 / 权限 / 信用 / 预警 / 抽查 / 差评工单 / 审计追溯 / 日统计 / 审批缓存 / 数据字典 / 操作日志')
changes.append('第18页: 企业端 9→17、监管端 15→16 及主要表')

# ---------- 第 19 页：OCR 方向修正 ----------
s19 = slides[18]
for sh in s19.shapes:
    if sh.has_text_frame and '个接口契约' in sh.text_frame.text:
        set_text(sh, '跨端协同：核心接口契约把四端串起来')
        changes.append('第19页: 标题去掉硬编码数字')
tbl = find(s19, lambda x: x.has_table).table
set_cell(tbl, 8, 1, '审批端')
set_cell(tbl, 8, 2, '企业端')
set_cell(tbl, 8, 5, 'OCR 识别（审批端调用企业端 OCR 核验材料）')
changes.append('第19页: OCR 接口方向 企业端→审批端 修正为 审批端→企业端')

# ---------- 第 20 页：MQ 重试描述 ----------
s20 = slides[19]
for sh in s20.shapes:
    if not sh.has_text_frame:
        continue
    tf = sh.text_frame
    for i, para in enumerate(tf.paragraphs):
        if '失败重试 3 次' in para.text:
            set_para(sh, i, 'MQ 异步投递（队列持久化，保证不丢）；全程审计留痕')
            changes.append('第20页: 去掉"失败重试3次"，改为队列持久化')
            break

# ---------- 第 24 页：非功能指标改为与实现一致 ----------
s24 = slides[23]
tbl = find(s24, lambda x: x.has_table).table
set_cell(tbl, 1, 1, '首屏加载 ≤ 3s；API 响应 P95 ≤ 500ms（设计目标）')
set_cell(tbl, 2, 1, '密码 BCrypt 加盐哈希存储；接口按 RBAC 权限码拦截；敏感字段分级保护（字段级加密与限流列为后续演进）')
set_cell(tbl, 4, 1, '系统可用性目标 ≥ 99.5%；Feign 调用失败自动降级，不阻断主流程')
changes.append('第24页: 安全/性能/可用性 改为与实现一致的表述')

p.save(SRC)
print('已修改并保存：', SRC)
print('备份：', BAK)
print()
for c in changes:
    print(' -', c)
