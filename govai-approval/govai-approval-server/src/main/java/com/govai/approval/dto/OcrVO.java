package com.govai.approval.dto;

import lombok.Data;

import java.util.Map;

/**
 * OCR 识别响应
 */
@Data
public class OcrVO {
    /** 提取字段 */
    private Map<String, Object> fields;
    /** 整体置信度 0-1 */
    private Double confidence;
}
