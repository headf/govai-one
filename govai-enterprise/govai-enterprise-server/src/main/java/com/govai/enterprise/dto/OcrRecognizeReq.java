package com.govai.enterprise.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * OCR 识别请求(契约接口8: POST /api/v1/ocr/recognize,企业端对外提供)
 */
@Data
@Schema(description = "OCR识别请求")
public class OcrRecognizeReq {

    @Schema(description = "图片 Base64(可含 data:image/xxx;base64, 前缀)")
    private String imageBase64;

    @Schema(description = "识别类型 BUSINESS_LICENSE/ID_CARD/GENERAL")
    private String type;
}
