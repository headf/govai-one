package com.govai.approval.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 审批任务处理请求体
 */
@Data
public class HandleTaskReq {
    @NotNull(message = "任务ID不能为空")
    private Long taskId;
    /** PASS / REJECT / RETURN */
    @NotBlank(message = "审批结果不能为空")
    private String approveResult;
    /** 审批意见 */
    private String approveOpinion;
    /** 办理附加信息(JSON):办结岗填写证照/批文号、送达方式等 */
    private String handleDetail;
    /** 是否采纳AI建议 */
    private Boolean adoptAiAdvice;
}
