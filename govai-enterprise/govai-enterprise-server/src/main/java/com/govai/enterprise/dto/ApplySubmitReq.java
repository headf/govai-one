package com.govai.enterprise.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 申报提交请求(企业申报)
 */
@Data
@Schema(description = "申报提交请求")
public class ApplySubmitReq {

    @Schema(description = "事项ID")
    @NotNull(message = "事项不能为空")
    private Long matterId;

    @Schema(description = "草稿记录ID(续填草稿时传入,存在则更新原草稿而非新建)")
    private Long recordId;

    @Schema(description = "表单数据(键=form_config 的 key, 值=填写内容)")
    @NotNull(message = "表单数据不能为空")
    private Map<String, Object> formData;

    @Schema(description = "已上传材料清单(上传接口返回的 materialId 列表,可空)")
    private List<Long> materialIds;

    @Schema(description = "是否已通过合规预检(由前端调预检接口后置为 true 提交)")
    private Boolean preChecked = false;
}
