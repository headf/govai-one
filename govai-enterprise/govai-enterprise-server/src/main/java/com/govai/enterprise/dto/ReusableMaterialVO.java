package com.govai.enterprise.dto;

import com.govai.enterprise.entity.EnterpriseLicense;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 可复用证照/材料(供材料上传环节从已有证件中选择)
 */
@Data
@Schema(description = "可复用证照/材料")
public class ReusableMaterialVO {

    @Schema(description = "企业电子证照(含生成后的 fileUrl)")
    private List<EnterpriseLicense> licenses;

    @Schema(description = "历史已上传材料(按材料名去重)")
    private List<HistoryMaterial> history;

    @Data
    @Schema(description = "历史已上传材料")
    public static class HistoryMaterial {
        @Schema(description = "材料ID")
        private Long materialId;
        @Schema(description = "材料名称")
        private String materialName;
        @Schema(description = "文件URL")
        private String fileUrl;
        @Schema(description = "上传时间")
        private LocalDateTime uploadTime;
    }
}
