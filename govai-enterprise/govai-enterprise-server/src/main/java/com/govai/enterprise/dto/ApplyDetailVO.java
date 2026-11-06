package com.govai.enterprise.dto;

import com.govai.enterprise.entity.EntApplicationMaterial;
import com.govai.enterprise.entity.EntComplianceCheck;
import com.govai.enterprise.entity.SubsidyPayment;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 申报详情 VO(记录+材料+预检+进度+办结产出)
 */
@Data
@Schema(description = "申报详情")
public class ApplyDetailVO {

    @Schema(description = "申报记录")
    private ApplyRecordVO record;

    @Schema(description = "已上传材料")
    private List<EntApplicationMaterial> materials;

    @Schema(description = "合规预检项")
    private List<EntComplianceCheck> checks;

    @Schema(description = "审批进度(审批端返回)")
    private ApprovalProgressResp progress;

    @Schema(description = "补贴/减免发放记录(补贴类事项办结到账)")
    private List<SubsidyPayment> subsidies;
}
