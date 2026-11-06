package com.govai.citizen.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("application_record")
public class ApplicationRecord {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String recordCode;
    private Long matterId;
    private Long userId;
    private String applicantName;
    private String applicantPhone;
    private String applicantIdCard;
    private String formData;
    private String status;
    private Long approvalInstanceId;
    private LocalDateTime submitTime;
    private LocalDateTime finishTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
