package com.govai.enterprise.service;

import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.govai.enterprise.common.BusinessException;
import com.govai.enterprise.dto.OcrRecognizeReq;
import com.govai.enterprise.dto.OcrRecognizeResp;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * OCR 识别服务(调用本地 PaddleOCR Python HTTP 服务)
 * Python 服务: ocr-service/ocr_service.py,监听 8090
 */
@Slf4j
@Service
public class OcrService {

    @Value("${govai.ocr.url:http://localhost:8090}")
    private String ocrUrl;

    /**
     * 调用 OCR 识别(对外契约接口8 的核心逻辑)
     */
    public OcrRecognizeResp recognize(OcrRecognizeReq req) {
        if (req == null || req.getImageBase64() == null || req.getImageBase64().isBlank()) {
            throw new BusinessException(400, "图片不能为空");
        }
        Map<String, Object> payload = new java.util.HashMap<>();
        payload.put("imageBase64", req.getImageBase64());
        payload.put("type", req.getType() == null ? "GENERAL" : req.getType());
        try {
            // 调用 Python OCR 服务
            String respStr = HttpUtil.post(ocrUrl + "/recognize", JSONUtil.toJsonStr(payload), 60000);
            JSONObject resp = JSONUtil.parseObj(respStr);
            int code = resp.getInt("code", 500);
            if (code != 200) {
                log.error("OCR识别失败: {}", resp.getStr("message"));
                throw new BusinessException("OCR识别失败: " + resp.getStr("message"));
            }
            JSONObject data = resp.getJSONObject("data");
            if (data == null) {
                throw new BusinessException("OCR识别返回数据为空");
            }
            return convert(data);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("调用 OCR 服务异常(可能服务未启动)", e);
            throw new BusinessException("OCR 服务不可用,请确认 ocr-service 已启动");
        }
    }

    private OcrRecognizeResp convert(JSONObject data) {
        OcrRecognizeResp resp = new OcrRecognizeResp();
        resp.setType(data.getStr("type"));
        resp.setConfidence(data.getDouble("confidence"));
        // fields
        JSONObject fields = data.getJSONObject("fields");
        if (fields != null) {
            resp.setFields(fields);
        }
        // lines
        Object linesObj = data.get("lines");
        if (linesObj instanceof List<?> list) {
            List<OcrRecognizeResp.Line> lines = list.stream().map(o -> {
                JSONObject line = JSONUtil.parseObj(o);
                OcrRecognizeResp.Line l = new OcrRecognizeResp.Line();
                l.setText(line.getStr("text"));
                l.setConfidence(line.getDouble("confidence"));
                return l;
            }).collect(Collectors.toList());
            resp.setLines(lines);
        }
        return resp;
    }
}
