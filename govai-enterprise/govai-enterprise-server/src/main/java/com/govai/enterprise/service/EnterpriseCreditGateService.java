package com.govai.enterprise.service;

import cn.hutool.core.util.StrUtil;
import com.govai.enterprise.dto.CreditVO;
import com.govai.enterprise.entity.EntMatterInfo;
import com.govai.enterprise.feign.SupervisionClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.regex.Pattern;

/**
 * 企业信用门禁服务(守信激励 / 失信受限)
 *
 * 真实语义:信用好(A级)→申办全事项走绿色通道(激励提示);信用差(D级)→
 * 申办补贴扶持类事项受限(拦截),需先办理信用修复。信用数据实时查监管端(权威),
 * 监管端离线时 fail-open(信用未知=不拦)。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EnterpriseCreditGateService {

    private final SupervisionClient supervisionClient;

    /** 补贴扶持类事项判定词:categoryId==4 或 名称含补贴/减免/稳岗 */
    private static final Pattern SUBSIDY_PATTERN = Pattern.compile("补贴|减免|稳岗|扶持|惠企");

    /**
     * 信用判定结果
     */
    public static class GateResult {
        /** 信用等级 A/B/C/D 或 UNKNOWN(未建档/监管端离线) */
        public final String level;
        /** 是否补贴扶持类事项 */
        public final boolean subsidy;
        /** 是否受限(需拦截):D 级 且 补贴类 */
        public final boolean restricted;
        /** 是否守信绿色通道(A 级) */
        public final boolean greenChannel;

        public GateResult(String level, boolean subsidy, boolean restricted, boolean greenChannel) {
            this.level = level;
            this.subsidy = subsidy;
            this.restricted = restricted;
            this.greenChannel = greenChannel;
        }
    }

    /**
     * 查本企业信用并判定当前事项是否受限
     */
    public GateResult evaluate(Long enterpriseId, EntMatterInfo matter) {
        String level = fetchLevel(enterpriseId);
        boolean subsidy = isSubsidy(matter);
        boolean restricted = "D".equals(level) && subsidy;
        boolean green = "A".equals(level);
        return new GateResult(level, subsidy, restricted, green);
    }

    /** 查企业信用等级;查不到/监管端离线返回 UNKNOWN(fail-open 不拦) */
    private String fetchLevel(Long enterpriseId) {
        try {
            CreditVO vo = supervisionClient.myCredit(enterpriseId).getData();
            if (vo == null || StrUtil.isBlank(vo.getCreditLevel())) {
                return "UNKNOWN";
            }
            return vo.getCreditLevel();
        } catch (Exception e) {
            log.warn("[信用门禁] 查信用失败,按未知处理(不拦) enterpriseId={}", enterpriseId, e);
            return "UNKNOWN";
        }
    }

    /** 是否补贴扶持类事项(categoryId==4 或 名称关键词) */
    public boolean isSubsidy(EntMatterInfo matter) {
        if (matter == null) {
            return false;
        }
        if (matter.getCategoryId() != null && matter.getCategoryId() == 4L) {
            return true;
        }
        String name = matter.getMatterName();
        return name != null && SUBSIDY_PATTERN.matcher(name).find();
    }
}