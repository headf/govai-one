package com.govai.approval.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 规则引擎审核结果
 */
@Data
public class RuleResultVO {
    /** PASS / FAIL / TO_MANUAL */
    private String result;
    /** 失败原因列表 */
    private List<String> errors = new ArrayList<>();
    /** 各项规则明细 */
    private List<RuleItemResult> items = new ArrayList<>();

    @Data
    public static class RuleItemResult {
        private String fieldName;
        private String ruleType;
        private String operator;
        private String compareValue;
        private boolean passed;
        private String errorMessage;
    }
}
