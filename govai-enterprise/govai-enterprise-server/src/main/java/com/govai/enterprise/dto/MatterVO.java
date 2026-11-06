package com.govai.enterprise.dto;

import cn.hutool.json.JSONUtil;
import com.govai.enterprise.entity.EntMatterInfo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 法人事项 VO(列表/详情通用)
 * 将实体中的 JSON 文本解析为结构化对象返回前端
 */
@Data
@Schema(description = "法人事项")
public class MatterVO {

    @Schema(description = "事项ID")
    private Long id;

    @Schema(description = "事项编码")
    private String matterCode;

    @Schema(description = "事项名称")
    private String matterName;

    @Schema(description = "分类ID")
    private Long categoryId;

    @Schema(description = "分类名称")
    private String categoryName;

    @Schema(description = "实施部门")
    private String department;

    @Schema(description = "事项描述")
    private String description;

    @Schema(description = "办理指南")
    private String guideContent;

    @Schema(description = "申报条件")
    private List<String> conditions;

    @Schema(description = "所需材料")
    private List<String> materials;

    @Schema(description = "办理流程")
    private List<String> processSteps;

    @Schema(description = "法定办结天数")
    private Integer deadlineDays;

    @Schema(description = "申报表单配置(字段数组,前端动态渲染)")
    private List<FormFieldConfig> formConfig;

    public static MatterVO from(EntMatterInfo m, String categoryName) {
        MatterVO vo = new MatterVO();
        vo.setId(m.getId());
        vo.setMatterCode(m.getMatterCode());
        vo.setMatterName(m.getMatterName());
        vo.setCategoryId(m.getCategoryId());
        vo.setCategoryName(categoryName);
        vo.setDepartment(m.getDepartment());
        vo.setDescription(m.getDescription());
        vo.setGuideContent(m.getGuideContent());
        vo.setConditions(parseList(m.getConditions()));
        vo.setMaterials(parseList(m.getMaterials()));
        vo.setProcessSteps(parseList(m.getProcessSteps()));
        vo.setDeadlineDays(m.getDeadlineDays());
        vo.setFormConfig(parseForm(m.getFormConfig()));
        return vo;
    }

    private static List<String> parseList(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return JSONUtil.toList(json, String.class);
        } catch (Exception e) {
            return null;
        }
    }

    private static List<FormFieldConfig> parseForm(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return JSONUtil.toList(json, FormFieldConfig.class);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 动态表单字段配置(对应 ent_matter_info.form_config 的单条)
     */
    @Data
    public static class FormFieldConfig {
        @Schema(description = "字段名(与 formData 键对应)")
        private String key;
        @Schema(description = "字段标签")
        private String label;
        @Schema(description = "类型: text/number/date/select/textarea/checkbox")
        private String type;
        @Schema(description = "是否必填")
        private Boolean required;
        @Schema(description = "select 选项")
        private List<String> options;
        @Schema(description = "关联 OCR 识别类型(如 BUSINESS_LICENSE/ID_CARD),填了则支持拍照回填")
        private String ocr;
    }
}
