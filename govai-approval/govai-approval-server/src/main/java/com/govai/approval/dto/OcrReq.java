package com.govai.approval.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * OCR 识别请求（接口8：POST /api/v1/ocr/recognize）
 * 由企业端提供，审批端通过 Feign 调用
 */
@Data
public class OcrReq {
    /** 图片 Base64（含 data:image/xxx;base64, 前缀） */
    @NotBlank(message = "图片不能为空")
    private String imageBase64;
    /** BUSINESS_LICENSE / ID_CARD / FOOD_LICENSE */
    private String type;
}
