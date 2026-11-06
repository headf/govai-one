package com.govai.enterprise.controller;

import com.govai.enterprise.common.Result;
import com.govai.enterprise.dto.OcrRecognizeReq;
import com.govai.enterprise.dto.OcrRecognizeResp;
import com.govai.enterprise.service.OcrService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * OCR 识别接口(契约接口8,企业端对外提供,供审批端调用)
 * 路径 /api/v1/ocr/recognize,已在白名单(审批端通过 API 密钥/内网调用)
 */
@Tag(name = "OCR识别")
@RestController
@RequestMapping("/api/v1/ocr")
@RequiredArgsConstructor
public class OcrController {

    private final OcrService ocrService;

    @Operation(summary = "OCR识别(营业执照/身份证/通用)")
    @PostMapping("/recognize")
    public Result<OcrRecognizeResp> recognize(@RequestBody OcrRecognizeReq req) {
        return Result.success(ocrService.recognize(req));
    }
}
