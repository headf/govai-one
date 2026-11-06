package com.govai.enterprise.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 复用已有材料请求
 */
@Data
@Schema(description = "复用已有材料请求")
public class ReuseMaterialReq {

    @Schema(description = "材料名称")
    @NotBlank(message = "材料名称不能为空")
    private String materialName;

    @Schema(description = "已有文件URL(证照或历史材料)")
    @NotBlank(message = "文件URL不能为空")
    private String fileUrl;
}
