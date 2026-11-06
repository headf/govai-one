package com.govai.citizen.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.govai.citizen.common.BizException;
import com.govai.citizen.common.Constants;
import com.govai.citizen.config.UserContext;
import com.govai.citizen.dto.EvaluationStatsVO;
import com.govai.citizen.dto.EvaluationSubmitReq;
import com.govai.citizen.dto.EvaluationVO;
import com.govai.citizen.entity.ApplicationRecord;
import com.govai.citizen.entity.Evaluation;
import com.govai.citizen.entity.MatterInfo;
import com.govai.citizen.feign.SupervisionClient;
import com.govai.citizen.mapper.ApplicationRecordMapper;
import com.govai.citizen.mapper.EvaluationMapper;
import com.govai.citizen.mapper.MatterInfoMapper;
import com.govai.citizen.util.JsonUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class EvaluationService {

    private final EvaluationMapper evaluationMapper;
    private final ApplicationRecordMapper recordMapper;
    private final MatterInfoMapper matterMapper;
    private final SupervisionClient supervisionClient;

    public void submit(EvaluationSubmitReq req) {
        Long uid = UserContext.requireUserId();
        ApplicationRecord r = recordMapper.selectById(req.getRecordId());
        if (r == null || !r.getUserId().equals(uid)) {
            throw new BizException(404, "申报记录不存在");
        }
        if (!Constants.STATUS_APPROVED.equals(r.getStatus())) {
            throw new BizException("办件办结后才能评价");
        }
        long exists = evaluationMapper.selectCount(new LambdaQueryWrapper<Evaluation>()
                .eq(Evaluation::getRecordId, r.getId())
                .eq(Evaluation::getUserId, uid));
        if (exists > 0) {
            throw new BizException("该办件已评价过");
        }
        Evaluation e = new Evaluation();
        e.setRecordId(r.getId());
        e.setUserId(uid);
        e.setStarRating(req.getStarRating());
        e.setContent(req.getContent());
        e.setTags(JsonUtil.fromList(req.getTags()));
        evaluationMapper.insert(e);

        // 差评(1-2 星)上报监管端,生成"差评整改工单" —— 好差评整改闭环;
        // 上报失败只记日志,不影响评价本身已提交的事实(容错,不阻断主流程)
        if (req.getStarRating() != null && req.getStarRating() <= 2) {
            try {
                MatterInfo m = matterMapper.selectById(r.getMatterId());
                Map<String, Object> body = new HashMap<>();
                body.put("source", "CITIZEN");
                body.put("applicantName", r.getApplicantName());
                body.put("matterName", m == null ? null : m.getMatterName());
                body.put("recordCode", r.getRecordCode());
                body.put("starRating", req.getStarRating());
                body.put("content", req.getContent());
                body.put("tags", req.getTags() == null ? null : String.join(",", req.getTags()));
                supervisionClient.reportReviewOrder(body);
                log.info("差评已上报监管端生成整改工单 recordId={} star={}", r.getId(), req.getStarRating());
            } catch (Exception ex) {
                log.warn("差评上报监管端失败(忽略,不影响评价提交): {}", ex.getMessage());
            }
        }
    }

    public List<EvaluationVO> my() {
        Long uid = UserContext.requireUserId();
        List<Evaluation> list = evaluationMapper.selectList(new LambdaQueryWrapper<Evaluation>()
                .eq(Evaluation::getUserId, uid)
                .orderByDesc(Evaluation::getCreateTime));
        return list.stream().map(e -> {
            EvaluationVO vo = new EvaluationVO();
            vo.setId(e.getId());
            vo.setRecordId(e.getRecordId());
            vo.setStarRating(e.getStarRating());
            vo.setContent(e.getContent());
            vo.setTags(JsonUtil.toList(e.getTags()));
            vo.setCreateTime(e.getCreateTime());
            ApplicationRecord r = recordMapper.selectById(e.getRecordId());
            if (r != null) {
                MatterInfo m = matterMapper.selectById(r.getMatterId());
                vo.setMatterName(m == null ? "" : m.getMatterName());
            }
            return vo;
        }).collect(Collectors.toList());
    }

    public EvaluationStatsVO stats() {
        List<Evaluation> all = evaluationMapper.selectList(null);
        EvaluationStatsVO vo = new EvaluationStatsVO();
        if (all.isEmpty()) {
            return vo;
        }
        long five = 0, four = 0, three = 0, two = 0, one = 0, sum = 0;
        for (Evaluation e : all) {
            int star = e.getStarRating() == null ? 0 : e.getStarRating();
            sum += star;
            switch (star) {
                case 5 -> five++;
                case 4 -> four++;
                case 3 -> three++;
                case 2 -> two++;
                default -> one++;
            }
        }
        vo.setTotal(all.size());
        vo.setAvgStar(Math.round((sum * 10.0) / all.size()) / 10.0);
        vo.setFiveStar(five);
        vo.setFourStar(four);
        vo.setThreeStar(three);
        vo.setTwoStar(two);
        vo.setOneStar(one);
        vo.setPositiveRate(Math.round(((five + four) * 1000.0) / all.size()) / 10.0);
        return vo;
    }
}
