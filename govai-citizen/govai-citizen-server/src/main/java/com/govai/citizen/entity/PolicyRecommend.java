package com.govai.citizen.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("policy_recommend")
public class PolicyRecommend {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String policyTitle;
    private String policyContent;
    private String matchReason;
    private Integer isRead;
    private LocalDateTime createTime;
}
