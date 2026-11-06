package com.govai.supervision.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.govai.supervision.common.BizNoGenerator;
import com.govai.supervision.common.BusinessException;
import com.govai.supervision.common.PageResult;
import com.govai.supervision.entity.ApprovalDataCache;
import com.govai.supervision.entity.CreditEnterprise;
import com.govai.supervision.entity.SupervisionSample;
import com.govai.supervision.mapper.ApprovalDataCacheMapper;
import com.govai.supervision.mapper.CreditEnterpriseMapper;
import com.govai.supervision.mapper.SupervisionSampleMapper;
import com.govai.supervision.service.SupervisionSampleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 抽样检查服务实现
 */
@Service
@RequiredArgsConstructor
public class SupervisionSampleServiceImpl implements SupervisionSampleService {

    private final SupervisionSampleMapper supervisionSampleMapper;
    private final ApprovalDataCacheMapper approvalDataCacheMapper;
    private final CreditEnterpriseMapper creditEnterpriseMapper;

    @Override
    public PageResult<SupervisionSample> pageSamples(long page, long size, String sampleType, String checkResult) {
        LambdaQueryWrapper<SupervisionSample> wrapper = new LambdaQueryWrapper<SupervisionSample>()
                .eq(sampleType != null && !sampleType.isBlank(), SupervisionSample::getSampleType, sampleType)
                .eq(checkResult != null && !checkResult.isBlank(), SupervisionSample::getCheckResult, checkResult)
                .orderByDesc(SupervisionSample::getCheckTime);
        return PageResult.of(supervisionSampleMapper.selectPage(new Page<>(page, size), wrapper));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void randomSample(int count, Long checkerId) {
        randomSample(count, checkerId, false);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void randomSample(int count, Long checkerId, boolean creditWeighted) {
        if (count <= 0 || count > 100) {
            throw new BusinessException(400, "抽样数量需在1-100之间");
        }
        // 已抽过的实例
        Set<Long> sampled = supervisionSampleMapper.selectList(null).stream()
                .filter(s -> s.getInstanceId() != null)
                .map(SupervisionSample::getInstanceId)
                .collect(Collectors.toSet());
        // 待抽样候选：审批缓存中未被抽过的实例
        List<ApprovalDataCache> candidates = approvalDataCacheMapper.selectList(null).stream()
                .filter(c -> !sampled.contains(c.getInstanceId()))
                .collect(Collectors.toList());
        if (creditWeighted) {
            candidates = weightedByCredit(candidates);
        }
        List<ApprovalDataCache> picked = candidates.stream().limit(count).collect(Collectors.toList());
        if (picked.isEmpty()) {
            throw new BusinessException(400, "没有可抽样的待检实例");
        }
        for (ApprovalDataCache c : picked) {
            SupervisionSample s = new SupervisionSample();
            s.setSampleNo(BizNoGenerator.gen("CJ"));
            s.setInstanceId(c.getInstanceId());
            s.setSampleType("RANDOM");
            s.setCheckResult("PASS"); // 随机预置通过，检查人可后续改
            String creditMark = c.getEnterpriseId() == null ? "" : creditMark(c.getEnterpriseId());
            s.setCheckDetail("随机抽查办件【" + c.getMatterName() + "】" + creditMark);
            s.setCheckerId(checkerId);
            s.setCheckTime(LocalDateTime.now());
            supervisionSampleMapper.insert(s);
        }
    }

    /**
     * 按申报企业信用等级差异化排序:D级最优先 > C > B > A > 未知/无企业关联。
     * 需求8.2:抽取比例按信用等级差异化,低信用主体优先被抽、纳入重点监管。
     */
    private List<ApprovalDataCache> weightedByCredit(List<ApprovalDataCache> candidates) {
        Set<Long> entIds = candidates.stream()
                .map(ApprovalDataCache::getEnterpriseId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());
        if (entIds.isEmpty()) {
            return candidates;
        }
        Map<Long, CreditEnterprise> credits = creditEnterpriseMapper.selectList(null).stream()
                .filter(c -> entIds.contains(c.getEnterpriseId()))
                .collect(Collectors.toMap(CreditEnterprise::getEnterpriseId, Function.identity(), (a, b) -> a));
        return candidates.stream()
                .sorted(Comparator
                        .comparingInt((ApprovalDataCache c) -> creditRank(credits.get(c.getEnterpriseId())))
                        .thenComparing(ApprovalDataCache::getInstanceId))
                .collect(Collectors.toList());
    }

    /** D=0 > C=1 > B=2 > A=3 > 未知/无档案=4(数值越小越优先被抽) */
    private int creditRank(CreditEnterprise credit) {
        if (credit == null || credit.getCreditLevel() == null) {
            return 4;
        }
        return switch (credit.getCreditLevel()) {
            case "D" -> 0;
            case "C" -> 1;
            case "B" -> 2;
            case "A" -> 3;
            default -> 4;
        };
    }

    /** D 级命中标记重点优先,便于检查人识别差异化抽查结果 */
    private String creditMark(Long enterpriseId) {
        CreditEnterprise credit = creditEnterpriseMapper.selectOne(
                new LambdaQueryWrapper<CreditEnterprise>().eq(CreditEnterprise::getEnterpriseId, enterpriseId));
        if (credit != null && "D".equals(credit.getCreditLevel())) {
            return "(D级·重点优先)";
        }
        return "";
    }

    @Override
    public SupervisionSample targetedSample(Long instanceId, String checkResult, String checkDetail, Long checkerId) {
        SupervisionSample s = new SupervisionSample();
        s.setSampleNo(BizNoGenerator.gen("CJ"));
        s.setInstanceId(instanceId);
        s.setSampleType("TARGETED");
        s.setCheckResult(checkResult);
        s.setCheckDetail(checkDetail);
        s.setCheckerId(checkerId);
        s.setCheckTime(LocalDateTime.now());
        supervisionSampleMapper.insert(s);
        return s;
    }
}