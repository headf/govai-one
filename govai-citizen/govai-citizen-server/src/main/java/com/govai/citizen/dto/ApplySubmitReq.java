package com.govai.citizen.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class ApplySubmitReq {

    @NotNull(message = "事项ID不能为空")
    private Long matterId;

    private String applicantName;
    private String applicantPhone;
    private String applicantIdCard;

    private Map<String, Object> formData;

    private List<MaterialUploadItem> materials;

    @Data
    public static class MaterialUploadItem {
        private String materialName;
        private String fileUrl;
        private String fileType;
    }
}
