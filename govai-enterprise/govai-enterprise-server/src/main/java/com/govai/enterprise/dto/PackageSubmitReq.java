package com.govai.enterprise.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 一件事套餐一表申报提交请求
 */
@Data
@Schema(description = "套餐一表申报提交请求")
public class PackageSubmitReq {

    @Schema(description = "套餐ID(由路径提供,无需在请求体传)")
    private Long packageId;

    @Schema(description = "合并表单数据")
    @NotNull(message = "表单数据不能为空")
    private Map<String, Object> formData;

    @Schema(description = "已备齐的合并材料清单(名称与文件URL)")
    private List<Material> materials;

    @Data
    @Schema(description = "合并清单中的一份材料")
    public static class Material {
        @Schema(description = "材料名称(对应套餐合并清单中的项)")
        private String materialName;

        @Schema(description = "文件URL(新上传或从已有复用的已有文件)")
        private String fileUrl;
    }
}
