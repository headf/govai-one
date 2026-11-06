# -*- coding: utf-8 -*-
"""
GovAI One 企业端 - PaddleOCR 识别服务
提供营业执照/身份证/通用图片的文字识别 HTTP 接口,供 Spring Boot 后端与审批端调用。

启动: py -3.11 ocr_service.py    (监听 8090)
依赖: paddlepaddle==2.6.2, paddleocr==2.8.1, flask
"""
import base64
import io
import os
import re
import sys

os.environ.setdefault("KMP_DUPLICATE_LIB_OK", "TRUE")
sys.stdout.reconfigure(encoding="utf-8")

from flask import Flask, request, jsonify
from paddleocr import PaddleOCR

app = Flask(__name__)

# 全局单例 OCR 实例(启动时加载模型一次,避免每次请求重新加载)
_ocr = None


def get_ocr():
    global _ocr
    if _ocr is None:
        _ocr = PaddleOCR(use_angle_cls=True, lang="ch", show_log=False)
    return _ocr


def decode_image(image_base64):
    """解析 data:image/xxx;base64,xxx 或纯 base64,返回 PIL Image"""
    if not image_base64:
        raise ValueError("imageBase64 不能为空")
    data = image_base64
    if "," in data and data.strip().lower().startswith("data:"):
        data = data.split(",", 1)[1]
    raw = base64.b64decode(data)
    from PIL import Image
    return Image.open(io.BytesIO(raw))


# 中文大写/小写数字 -> 阿拉伯数字(用于注册资本解析)
_CN_NUM = {'零': 0, '〇': 0, '一': 1, '二': 2, '两': 2, '三': 3, '四': 4, '五': 5,
           '六': 6, '七': 7, '八': 8, '九': 9,
           '壹': 1, '贰': 2, '叁': 3, '肆': 4, '伍': 5, '陆': 6, '柒': 7, '捌': 8, '玖': 9}
_CN_UNIT = {'十': 10, '拾': 10, '百': 100, '佰': 100, '千': 1000, '仟': 1000}


def _cn_to_int(s):
    total = section = num = 0
    for ch in s:
        if ch in _CN_NUM:
            num = _CN_NUM[ch]
        elif ch in _CN_UNIT:
            section += (num if num else 1) * _CN_UNIT[ch]
            num = 0
        elif ch in ('万', '萬'):
            section = (section + num) * 10000
            total += section
            section = num = 0
        elif ch in ('亿', '億'):
            section = (section + num) * 100000000
            total += section
            section = num = 0
    return total + section + num


def _parse_number(s):
    if not s:
        return None
    if re.fullmatch(r"\d+(?:\.\d+)?", s):
        return float(s)
    return _cn_to_int(s)


def _fmt_number(n):
    if n is None:
        return ""
    if isinstance(n, float) and n.is_integer():
        return str(int(n))
    return str(n)


def parse_capital(val):
    """注册资本金额文本 -> 以万元为单位的数字字符串(如 '壹仟万元' -> '1000')"""
    if not val:
        return ""
    cleaned = re.sub(r"[^\d.零〇一二两三四五六七八九十百千万亿壹贰叁肆伍陆柒捌玖拾佰仟萬億]", "", val)
    if not cleaned:
        return ""
    if "万" in cleaned or "萬" in cleaned:
        head = re.split(r"[万亿萬億]", cleaned)[0]
        return _fmt_number(_parse_number(head))
    n = _parse_number(cleaned)
    if n is None:
        return ""
    return _fmt_number(n / 10000.0)


def extract_fields(lines):
    """
    将 OCR 识别出的文本行聚合成结构化字段。
    针对真实纸质营业执照版式做了适配:
      - 竖排标签(如 "名称" 被识别成 "名" / "称" 两行)通过整行拼接后匹配
      - 老版营业执照以 "注册号" 代替 "统一社会信用代码"
      - 注册资本支持中文大写金额(如 "壹仟万元" -> 1000)
    """
    texts = [l[1][0] for l in lines]
    full_text = "\n".join(texts)

    # 去掉空白与冒号后把整张图的文字拼成一条长串,这样被拆成多行的竖排标签能自然还原
    def norm(s):
        return re.sub(r"[\s:：]+", "", s)

    flat = "".join(norm(t) for t in texts)

    # 所有可能出现的标签,用于截断字段值(值 = 标签之后、下一个标签之前)
    boundaries = [
        "统一社会信用代码", "社会信用代码", "信用代码", "注册号",
        "企业名称", "公司名称", "名称",
        "企业类型", "类型",
        "住所", "经营场所", "地址",
        "法定代表人", "法人",
        "注册资本",
        "成立日期", "成立时间",
        "营业期限",
        "经营范围",
        "登记机关",
    ]

    def value_after(label):
        idx = flat.find(label)
        if idx == -1:
            return ""
        start = idx + len(label)
        end = len(flat)
        for nb in boundaries:
            if nb == label:
                continue
            p = flat.find(nb, start)
            if p != -1 and p < end:
                end = p
        return flat[start:end]

    def first_value(*keywords):
        for kw in keywords:
            v = value_after(kw)
            if v:
                return v
        return ""

    fields = {}
    fields["enterpriseName"] = first_value("企业名称", "公司名称", "名称")
    fields["creditCode"] = first_value("统一社会信用代码", "社会信用代码", "信用代码", "注册号")
    fields["legalPerson"] = first_value("法定代表人", "法人")
    fields["registerAddress"] = first_value("住所", "经营场所", "地址")
    fields["registerCapital"] = parse_capital(first_value("注册资本"))
    fields["establishDate"] = first_value("成立日期", "成立时间")
    fields["businessScope"] = first_value("经营范围")
    fields["idCardNo"] = first_value("公民身份号码", "身份证号", "证件号码")
    fields["name"] = first_value("姓名")
    fields["rawText"] = full_text
    return fields


@app.route("/health", methods=["GET"])
def health():
    return jsonify({"code": 200, "status": "ok", "engine": "paddleocr"})


@app.route("/recognize", methods=["POST"])
def recognize():
    try:
        data = request.get_json(force=True)
        image_base64 = data.get("imageBase64")
        recognize_type = data.get("type", "GENERAL")  # BUSINESS_LICENSE / ID_CARD / GENERAL
        img = decode_image(image_base64)
        ocr = get_ocr()
        # paddleocr 2.x 的 ocr() 只接受 ndarray/list/str/bytes,需将 PIL Image 转为 ndarray
        import numpy as np
        img_array = np.array(img)
        result = ocr.ocr(img_array, cls=True)
        lines = result[0] if result and result[0] else []
        fields = extract_fields(lines)
        # 计算平均置信度
        confs = [l[1][1] for l in lines]
        confidence = round(sum(confs) / len(confs), 4) if confs else 0.0
        lines_out = [{"text": l[1][0], "confidence": round(l[1][1], 4)} for l in lines]
        return jsonify({
            "code": 200,
            "message": "识别成功",
            "data": {
                "type": recognize_type,
                "fields": fields,
                "lines": lines_out,
                "confidence": confidence
            }
        })
    except Exception as e:
        import traceback
        traceback.print_exc()
        return jsonify({"code": 500, "message": "识别失败: " + repr(e), "data": None})


if __name__ == "__main__":
    port = int(os.environ.get("OCR_PORT", "8090"))
    print(f"[PaddleOCR Service] listening on http://localhost:{port}")
    app.run(host="0.0.0.0", port=port, debug=False, threaded=True)
