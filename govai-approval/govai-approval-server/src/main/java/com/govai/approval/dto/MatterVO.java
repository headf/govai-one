package com.govai.approval.dto;

import lombok.Data;

import java.util.List;

/**
 * 群众端事项信息（接口3：GET /api/v1/matters/{id}）
 * 由群众端提供，审批端通过 Feign 调用
 */
@Data
public class MatterVO {
    private Long id;
    private String matterCode;
    private String matterName;
    private String department;
    private String guideContent;
    private List<String> conditions;
    private List<String> materials;
    private Integer deadlineDays;
}
