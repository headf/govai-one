package com.govai.approval.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.govai.approval.common.Constants;
import com.govai.approval.dto.RuleResultVO;
import com.govai.approval.entity.ApprovalRuleItem;
import com.govai.approval.entity.ApprovalRuleSet;
import com.govai.approval.mapper.ApprovalRuleItemMapper;
import com.govai.approval.mapper.ApprovalRuleSetMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 规则引擎 Service
 *
 * 规则类型：NUMBER 数值比较 / TEXT 文本匹配 / DATE 日期判断 / LICENSE 证照库查询
 * 操作符：GT/LT/EQ/CONTAINS/BETWEEN
 * 规则间逻辑：AND（全过）/ OR（任一过）
 *
 * 三层智能审批决策中的第一层：规则引擎自动审核
 * 输出 PASS（自动通过）/ FAIL（拒绝，仍转人工最终判定）/ TO_MANUAL（转人工）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RuleEngineService {

    private final ApprovalRuleSetMapper ruleSetMapper;
    private final ApprovalRuleItemMapper ruleItemMapper;

    /**
     * 按事项类型查找规则集并执行
     * @param matterType 事项类型
     * @param formData   申报表单数据
     */
    public RuleResultVO checkByMatterType(String matterType, Map<String, Object> formData) {
        if (!StringUtils.hasText(matterType) || formData == null) {
            return toManual("未配置规则集或表单数据为空");
        }
        ApprovalRuleSet ruleSet = ruleSetMapper.selectOne(
                new LambdaQueryWrapper<ApprovalRuleSet>()
                        .eq(ApprovalRuleSet::getMatterType, matterType)
                        .eq(ApprovalRuleSet::getStatus, 1)
                        .last("LIMIT 1"));
        if (ruleSet == null) {
            return toManual("事项[" + matterType + "]未配置规则集");
        }
        return execute(ruleSet, formData);
    }

    /**
     * 按 ruleSetId 显式执行（管理员测试用）
     */
    public RuleResultVO checkByRuleSetId(Long ruleSetId, Map<String, Object> formData) {
        ApprovalRuleSet ruleSet = ruleSetMapper.selectById(ruleSetId);
        if (ruleSet == null) return toManual("规则集不存在");
        return execute(ruleSet, formData);
    }

    private RuleResultVO execute(ApprovalRuleSet ruleSet, Map<String, Object> formData) {
        List<ApprovalRuleItem> items = ruleItemMapper.selectList(
                new LambdaQueryWrapper<ApprovalRuleItem>()
                        .eq(ApprovalRuleItem::getRuleSetId, ruleSet.getId())
                        .orderByAsc(ApprovalRuleItem::getSortOrder));
        if (items.isEmpty()) {
            return toManual("规则集[" + ruleSet.getRuleSetName() + "]未配置规则项");
        }

        RuleResultVO result = new RuleResultVO();
        // 顶层逻辑：AND 全过才算过；OR 任一过即过
        String logic = StringUtils.hasText(ruleSet.getLogicType()) ? ruleSet.getLogicType() : "AND";
        boolean isAnd = "AND".equalsIgnoreCase(logic);

        boolean allPass = true;
        boolean anyPass = false;
        for (ApprovalRuleItem item : items) {
            RuleResultVO.RuleItemResult ir = evaluate(item, formData);
            result.getItems().add(ir);
            if (ir.isPassed()) {
                anyPass = true;
            } else {
                allPass = false;
                result.getErrors().add(ir.getErrorMessage());
            }
        }

        boolean pass = isAnd ? allPass : anyPass;
        if (pass) {
            result.setResult(Constants.RULE_PASS);
        } else {
            // 规则不通过 -> 失败，但本系统约定不直接拒绝，转人工最终判定
            result.setResult(Constants.RULE_FAIL);
        }
        log.info("规则集[{}]审核完成 result={} errors={}", ruleSet.getRuleSetName(), result.getResult(), result.getErrors());
        return result;
    }

    /**
     * 单条规则求值
     */
    private RuleResultVO.RuleItemResult evaluate(ApprovalRuleItem item, Map<String, Object> formData) {
        RuleResultVO.RuleItemResult ir = new RuleResultVO.RuleItemResult();
        ir.setFieldName(item.getFieldName());
        ir.setRuleType(item.getRuleType());
        ir.setOperator(item.getOperator());
        ir.setCompareValue(item.getCompareValue());
        ir.setErrorMessage(item.getErrorMessage());

        Object raw = formData == null ? null : formData.get(item.getFieldName());
        boolean ok;
        try {
            ok = switch (item.getRuleType() == null ? "" : item.getRuleType()) {
                case "NUMBER" -> compareNumber(raw, item.getOperator(), item.getCompareValue());
                case "TEXT"   -> compareText(raw, item.getOperator(), item.getCompareValue());
                case "DATE"   -> compareDate(raw, item.getOperator(), item.getCompareValue());
                case "LICENSE" -> checkLicense(raw, item.getOperator(), item.getCompareValue());
                default -> false;
            };
        } catch (Exception e) {
            log.warn("规则项求值异常 field={} err={}", item.getFieldName(), e.getMessage());
            ok = false;
        }
        ir.setPassed(ok);
        return ir;
    }

    private boolean compareNumber(Object raw, String op, String compareValue) {
        if (raw == null) return false;
        double actual = Double.parseDouble(String.valueOf(raw));
        double expected = Double.parseDouble(compareValue);
        return switch (op) {
            case "GT"       -> actual > expected;
            case "LT"       -> actual < expected;
            case "EQ"       -> Math.abs(actual - expected) < 1e-6;
            case "BETWEEN"  -> {
                String[] parts = compareValue.split(",");
                if (parts.length == 2) yield actual >= Double.parseDouble(parts[0]) && actual <= Double.parseDouble(parts[1]);
                yield false;
            }
            default -> false;
        };
    }

    private boolean compareText(Object raw, String op, String compareValue) {
        if (raw == null) return false;
        String actual = String.valueOf(raw);
        return switch (op) {
            case "EQ"        -> Objects.equals(actual, compareValue);
            case "CONTAINS" -> actual.contains(compareValue);
            default -> false;
        };
    }

    private boolean compareDate(Object raw, String op, String compareValue) {
        if (raw == null) return false;
        // 支持 ${today} 占位符
        String expected = compareValue.replace("${today}", LocalDate.now().toString());
        DateTimeFormatter[] fmts = {DateTimeFormatter.ISO_LOCAL_DATE, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")};
        LocalDateTime actual = parseDate(String.valueOf(raw), fmts);
        LocalDateTime expectedDate = parseDate(expected, fmts);
        if (actual == null || expectedDate == null) return false;
        return switch (op) {
            case "GT"       -> actual.isAfter(expectedDate);
            case "LT"       -> actual.isBefore(expectedDate);
            case "EQ"       -> actual.isEqual(expectedDate);
            case "BETWEEN"  -> {
                String[] parts = expected.split(",");
                if (parts.length == 2) {
                    LocalDateTime a = parseDate(parts[0], fmts), b = parseDate(parts[1], fmts);
                    yield a != null && b != null && !actual.isBefore(a) && !actual.isAfter(b);
                }
                yield false;
            }
            default -> false;
        };
    }

    private LocalDateTime parseDate(String s, DateTimeFormatter[] fmts) {
        for (DateTimeFormatter f : fmts) {
            try {
                if (s.length() <= 10) return LocalDate.parse(s, f).atStartOfDay();
                return LocalDateTime.parse(s, f);
            } catch (Exception ignored) {}
        }
        return null;
    }

    /**
     * 证照库查询（实训阶段简化：只要字段非空即认为有证）
     */
    private boolean checkLicense(Object raw, String op, String compareValue) {
        return raw != null && StringUtils.hasText(String.valueOf(raw));
    }

    private RuleResultVO toManual(String reason) {
        RuleResultVO r = new RuleResultVO();
        r.setResult(Constants.RULE_TO_MANUAL);
        r.getErrors().add(reason);
        return r;
    }
}
