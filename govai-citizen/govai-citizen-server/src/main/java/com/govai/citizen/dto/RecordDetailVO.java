package com.govai.citizen.dto;

import com.govai.citizen.entity.ApplicationMaterial;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class RecordDetailVO {

    private Long id;
    private String recordCode;
    private Long matterId;
    private String matterName;
    private String matterCode;
    private String department;
    private String status;
    private String statusText;
    private String applicantName;
    private String applicantPhone;
    private String applicantIdCard;
    private Map<String, Object> formData;
    private Long approvalInstanceId;
    private String submitTime;
    private String finishTime;
    private List<ApplicationMaterial> materials;
}
