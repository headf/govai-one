package com.govai.enterprise.dto;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 申报记录 VO
 */
@Data
@Schema(description = "申报记录")
public class ApplyRecordVO {

    @Schema(description = "记录ID")
    private Long id;

    @Schema(description = "申报编号")
    private String recordCode;

    @Schema(description = "事项ID")
    private Long matterId;

    @Schema(description = "事项名称")
    private String matterName;

    @Schema(description = "事项编码")
    private String matterCode;

    @Schema(description = "状态 PENDING/APPROVING/PASSED/REJECTED/REPAIR")
    private String status;

    @Schema(description = "审批实例ID(跨服务)")
    private Long approvalInstanceId;

    @Schema(description = "表单数据(解析为对象)")
    private Map<String, Object> formData;

    @Schema(description = "提交时间")
    private LocalDateTime submitTime;

    @Schema(description = "办结时间")
    private LocalDateTime finishTime;

    @Schema(description = "审批端退回/补正意见(REPAIR/REJECTED 时有值)")
    private String remark;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    public static ApplyRecordVO from(com.govai.enterprise.entity.EntApplicationRecord r, String matterName, String matterCode) {
        ApplyRecordVO vo = new ApplyRecordVO();
        vo.setId(r.getId());
        vo.setRecordCode(r.getRecordCode());
        vo.setMatterId(r.getMatterId());
        vo.setMatterName(matterName);
        vo.setMatterCode(matterCode);
        vo.setStatus(r.getStatus());
        vo.setApprovalInstanceId(r.getApprovalInstanceId());
        try {
            JSONObject obj = JSONUtil.parseObj(r.getFormData());
            vo.setFormData(obj);
        } catch (Exception e) {
            vo.setFormData(null);
        }
        vo.setSubmitTime(r.getSubmitTime());
        vo.setFinishTime(r.getFinishTime());
        vo.setRemark(r.getRemark());
        vo.setCreateTime(r.getCreateTime());
        return vo;
    }
}
