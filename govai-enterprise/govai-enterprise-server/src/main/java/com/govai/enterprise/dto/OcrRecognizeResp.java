package com.govai.enterprise.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * OCR 识别响应(契约接口8 返回体)
 */
@Data
@Schema(description = "OCR识别响应")
public class OcrRecognizeResp {

    @Schema(description = "识别类型")
    private String type;

    @Schema(description = "结构化字段(enterpriseName/creditCode/legalPerson/rawText 等)")
    private Map<String, Object> fields;

    @Schema(description = "识别置信度(0-1)")
    private Double confidence;

    @Schema(description = "识别文本行列表")
    private List<Line> lines;

    @Data
    public static class Line {
        @Schema(description = "文本")
        private String text;
        @Schema(description = "置信度")
        private Double confidence;
    }
}
