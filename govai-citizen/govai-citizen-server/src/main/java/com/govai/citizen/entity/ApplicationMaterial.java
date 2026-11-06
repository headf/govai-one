package com.govai.citizen.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("application_material")
public class ApplicationMaterial {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long recordId;
    private String materialName;
    private String fileUrl;
    private String fileType;
    private String ocrResult;
    private Integer ocrStatus;
    private LocalDateTime uploadTime;
}
