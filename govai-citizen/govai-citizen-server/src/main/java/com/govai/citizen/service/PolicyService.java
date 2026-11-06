package com.govai.citizen.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.govai.citizen.common.BizException;
import com.govai.citizen.common.Constants;
import com.govai.citizen.config.UserContext;
import com.govai.citizen.dto.PolicyVO;
import com.govai.citizen.entity.CitizenUser;
import com.govai.citizen.entity.PolicyRecommend;
import com.govai.citizen.entity.SiteMessage;
import com.govai.citizen.mapper.ApplicationRecordMapper;
import com.govai.citizen.mapper.CitizenUserMapper;
import com.govai.citizen.mapper.PolicyRecommendMapper;
import com.govai.citizen.mapper.SiteMessageMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 政策推荐：按实名画像(实名状态/年龄段/是否办过事)动态匹配并生成推荐；实名变化后重推。
 */
@Service
@RequiredArgsConstructor
public class PolicyService {

    private final PolicyRecommendMapper policyMapper;
    private final CitizenUserMapper userMapper;
    private final ApplicationRecordMapper recordMapper;
    private final SiteMessageMapper siteMessageMapper;

    public List<PolicyVO> list() {
        Long uid = UserContext.requireUserId();
        ensureSeeded(uid);
        return policyMapper.selectList(new LambdaQueryWrapper<PolicyRecommend>()
                        .eq(PolicyRecommend::getUserId, uid)
                        .orderByDesc(PolicyRecommend::getIsRead)
                        .orderByDesc(PolicyRecommend::getCreateTime))
                .stream().map(PolicyVO::from).collect(Collectors.toList());
    }

    public PolicyVO detail(Long id) {
        Long uid = UserContext.requireUserId();
        PolicyRecommend p = policyMapper.selectById(id);
        if (p == null || !p.getUserId().equals(uid)) {
            throw new BizException(404, "政策推荐不存在");
        }
        return PolicyVO.from(p);
    }

    public void markRead(Long id) {
        Long uid = UserContext.requireUserId();
        PolicyRecommend p = policyMapper.selectById(id);
        if (p == null || !p.getUserId().equals(uid)) {
            throw new BizException(404, "政策推荐不存在");
        }
        if (p.getIsRead() == null || p.getIsRead() == 0) {
            p.setIsRead(1);
            policyMapper.updateById(p);
        }
    }

    /** 实名/资料更新后按新画像重推 */
    @Transactional(rollbackFor = Exception.class)
    public void reseedForProfileChange(Long uid) {
        policyMapper.delete(new LambdaQueryWrapper<PolicyRecommend>().eq(PolicyRecommend::getUserId, uid));
        doSeed(uid, true);
    }

    /**
     * 首次访问无记录时, 依据实名信息匹配播种政策推荐
     */
    private void ensureSeeded(Long uid) {
        long cnt = policyMapper.selectCount(new LambdaQueryWrapper<PolicyRecommend>()
                .eq(PolicyRecommend::getUserId, uid));
        if (cnt > 0) {
            return;
        }
        doSeed(uid, false);
    }

    private void doSeed(Long uid, boolean isReseed) {
        CitizenUser user = userMapper.selectById(uid);
        if (user == null) {
            return;
        }
        String idCard = user.getIdCard();
        Integer ageBand = idCard == null ? null : ageBandOf(idCard); // 0少年 1青年 2中年 3老年
        long applied = recordMapper.selectCount(null); // 总办件量(仅影响文案语气, 无则演示兜底)
        boolean hasApplied = applied > 0;
        boolean realVerified = idCard != null && !idCard.isBlank();

        String base = realVerified ? "根据您的实名认证信息匹配。" : "完成实名认证后,推荐将更精准。";

        // 1 老年养老
        boolean elder = ageBand != null && ageBand >= 3;
        seed(uid, "城乡居民基本养老保险参保缴费补贴",
                "对按时缴费的城乡居民,政府按缴费档次给予补贴,多缴多得,缴费满15年可按月领取养老金。",
                (elder ? "您已年满60岁,符合养老待遇领取人群画像,建议核对缴费年限。"
                        : "城乡居民基本养老保险覆盖全体未就业城乡居民,") + base);
        // 2 灵活就业补贴
        seed(uid, "灵活就业人员社会保险补贴申领指南",
                "符合条件的灵活就业人员可申请社保补贴,补贴期限一般不超过3年。",
                "以灵活就业身份缴纳社保的群众均可申请。" + base);
        // 3 医保门诊统筹
        seed(uid, "居民医保门诊统筹报销新规",
                "居民医保门诊统筹年度报销限额提高,参保群众在定点机构门诊就医可按规定报销。",
                "已参加居民医保的群众可关注门诊报销待遇变化。" + base);
        // 4 青年/毕业就业(≤25岁)或中年技能培训
        boolean youth = ageBand != null && ageBand <= 1;
        seed(uid, youth ? "高校毕业生就业创业补贴" : "职业技能提升培训补贴",
                youth
                        ? "毕业年度内高校毕业生自主创业可申请一次性创业补贴,就业可享受社保补贴。"
                        : "参加职业技能培训并取得证书的劳动者,可按规定申领培训补贴。",
                (youth ? "您处于青年就业创业年龄段,适用青年就业扶持政策。"
                        : "培训补贴面向劳动年龄段劳动者,建议按需参加。") + base);
        // 5 办过事后推荐的关联提示
        seed(uid, "政务服务办事小贴士",
                "已办结事项可在线评价；材料清单、办理进度均可在「我的申报」实时查看，无需重复跑腿。",
                hasApplied ? "您近期有申报记录,建议关注办件进度与结果反馈。"
                        : "先办成一件事,政策推荐与办事服务会更贴合您。");
        // 首次播完, 站内信提示(避免每次打扰)
        if (!isReseed) {
            pushPolicyMessage(uid);
        }
    }

    private void pushPolicyMessage(Long uid) {
        SiteMessage m = new SiteMessage();
        m.setUserId(uid);
        m.setMessageType(Constants.MSG_POLICY);
        m.setTitle("已为您匹配新的政策推荐");
        m.setContent("根据您的信息已生成政策推荐,点击查看是否符合您的申领条件。");
        m.setIsRead(0);
        siteMessageMapper.insert(m);
    }

    /** 年龄档: 0≤16, 1(17-25), 2(26-59), 3≥60 */
    private Integer ageBandOf(String idCard) {
        try {
            int year = Integer.parseInt(idCard.substring(6, 10));
            int age = 2026 - year;
            if (age < 17) return 0;
            if (age <= 25) return 1;
            if (age <= 59) return 2;
            return 3;
        } catch (Exception e) {
            return null;
        }
    }

    private void seed(Long uid, String title, String content, String reason) {
        PolicyRecommend p = new PolicyRecommend();
        p.setUserId(uid);
        p.setPolicyTitle(title);
        p.setPolicyContent(content);
        p.setMatchReason(reason);
        p.setIsRead(0);
        policyMapper.insert(p);
    }
}
