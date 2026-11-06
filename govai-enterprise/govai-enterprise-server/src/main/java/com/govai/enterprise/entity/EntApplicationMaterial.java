package com.govai.enterprise.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 企业申报材料表 ent_application_material
 */
@Data
@TableName("ent_application_material")
public class EntApplicationMaterial {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 申报记录ID */
    private Long recordId;

    /** 材料名称 */
    private String materialName;

    /** 文件URL */
    private String fileUrl;

    /** 文件类型 */
    private String fileType;

    /** 上传时的原始文件名（上传后文件被 UUID 重命名，此列用于"材料名 vs 实际文件"相符性核对） */
    private String originalName;

    /** OCR识别结果(JSON) */
    private String ocrResult;

    /** 0未识别 1已识别 2识别失败 */
    private Integer ocrStatus;

    private LocalDateTime uploadTime;
}
