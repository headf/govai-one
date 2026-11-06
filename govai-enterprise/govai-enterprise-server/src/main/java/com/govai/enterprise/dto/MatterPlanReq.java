package com.govai.enterprise.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "智能办事规划请求")
public class MatterPlanReq {

    @Schema(description = "用户描述(如:我想开一家食品销售公司)")
    private String description;

    @Schema(description = "经营类型/行业标签(可选,如:食品销售/餐饮/便利店)")
    private String businessType;

    @Schema(description = "是否已取得营业执照(可选,用于过滤前置事项)")
    private Boolean hasLicense;
}
