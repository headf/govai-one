package com.govai.enterprise.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.govai.enterprise.common.BusinessException;
import com.govai.enterprise.entity.EntPolicyMatch;
import com.govai.enterprise.entity.EnterpriseInfo;
import com.govai.enterprise.mapper.EntPolicyMatchMapper;
import com.govai.enterprise.mapper.EnterpriseInfoMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 企业政策匹配服务(画像播种 + 免申即享确认闭环)
 * 首次访问按企业画像(类型/经营范围/注册资金)自动匹配, 无预置行时不落库为假数据。
 */
@Service
@RequiredArgsConstructor
public class PolicyService {

    private final EntPolicyMatchMapper policyMapper;
    private final EnterpriseInfoMapper enterpriseInfoMapper;

    /**
     * 企业待办/已享政策列表(懒播种: 空则按画像生成)
     */
    public List<EntPolicyMatch> listByEnterprise(Long enterpriseId) {
        ensureSeeded(enterpriseId);
        return policyMapper.selectList(new LambdaQueryWrapper<EntPolicyMatch>()
                .eq(EntPolicyMatch::getEnterpriseId, enterpriseId)
                .orderByDesc(EntPolicyMatch::getId));
    }

    private void ensureSeeded(Long enterpriseId) {
        Long cnt = policyMapper.selectCount(new LambdaQueryWrapper<EntPolicyMatch>()
                .eq(EntPolicyMatch::getEnterpriseId, enterpriseId));
        if (cnt != null && cnt > 0) {
            return;
        }
        EnterpriseInfo info = enterpriseInfoMapper.selectById(enterpriseId);
        if (info == null) {
            return;
        }
        boolean small = "有限责任公司".equals(info.getEnterpriseType())
                || info.getRegisterCapital() == null
                || info.getRegisterCapital().compareTo(new BigDecimal("10000000")) <= 0;
        String scope = info.getBusinessScope() == null ? "" : info.getBusinessScope();
        boolean tech = scope.contains("科技") || scope.contains("软件") || scope.contains("研发") || scope.contains("信息技术");
        boolean food = scope.contains("餐饮") || scope.contains("食品");
        boolean manufacture = scope.contains("制造") || scope.contains("生产") || scope.contains("贸易");

        seed(enterpriseId, "小微企业普惠性税收减免",
                "对月销售额/年应纳税所得额符合条件的小微企业, 可享受增值税与所得税减免。",
                small ? "贵企业注册资本较小, 符合小微企业普惠优惠画像。" : "根据贵企业登记信息匹配普惠性减免政策。",
                new BigDecimal("95"));
        if (tech) {
            seed(enterpriseId, "研发费用加计扣除",
                    "科技型企业研发投入可按比例加计扣除, 降低所得税负担。",
                    "贵企业主营包含科技/研发, 适用研发费用加计扣除。",
                    new BigDecimal("92"));
        } else if (food) {
            seed(enterpriseId, "餐饮行业质量安全提升补贴",
                    "对食品安全管理达标的餐饮主体给予规范提升补贴。",
                    "依据贵企业餐饮/食品经营范围匹配。",
                    new BigDecimal("90"));
        } else {
            seed(enterpriseId, manufacture ? "制造业中小微企业缓缴社保费" : "吸纳重点群体就业税收优惠",
                    "阶段性缓缴/吸纳重点群体就业可按规定享受税费优惠。",
                    "根据贵企业行业画像推荐。",
                    new BigDecimal("88"));
        }
        seed(enterpriseId, "失业保险稳岗返还",
                "不裁员少裁员且足额参保的企业可申请稳岗返还。",
                "参保企业通用, 依贵企业缴费信息匹配。",
                new BigDecimal("85"));
        seed(enterpriseId, "创业担保贷款贴息",
                "符合条件的小微企业申请创业担保贷款可获财政贴息。",
                small ? "小微企业群体适用创业担保贷款贴息。" : "可关注创业担保贷款支持政策。",
                new BigDecimal("80"));
    }

    private void seed(Long enterpriseId, String title, String content, String reason, BigDecimal score) {
        EntPolicyMatch p = new EntPolicyMatch();
        p.setEnterpriseId(enterpriseId);
        p.setPolicyTitle(title);
        p.setPolicyContent(content);
        p.setMatchScore(score);
        p.setIsApplied(0);
        policyMapper.insert(p);
    }

    /**
     * 免申即享确认
     */
    public EntPolicyMatch confirm(Long enterpriseId, Long policyId) {
        EntPolicyMatch policy = policyMapper.selectById(policyId);
        if (policy == null) {
            throw new BusinessException("政策不存在");
        }
        if (!policy.getEnterpriseId().equals(enterpriseId)) {
            throw new BusinessException(403, "无权操作该政策");
        }
        policy.setIsApplied(1);
        policy.setApplyTime(LocalDateTime.now());
        policyMapper.updateById(policy);
        return policy;
    }
}
