package com.govai.enterprise.service;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.govai.enterprise.common.BusinessException;
import com.govai.enterprise.common.LoginUser;
import com.govai.enterprise.common.PageResult;
import com.govai.enterprise.dto.EvaluationSubmitReq;
import com.govai.enterprise.dto.EvaluationVO;
import com.govai.enterprise.entity.EntApplicationRecord;
import com.govai.enterprise.entity.EntEvaluation;
import com.govai.enterprise.entity.EntMatterInfo;
import com.govai.enterprise.mapper.EntApplicationRecordMapper;
import com.govai.enterprise.mapper.EntEvaluationMapper;
import com.govai.enterprise.feign.SupervisionClient;
import com.govai.enterprise.mapper.EntMatterInfoMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 好差评服务(办结后评价;差评上报监管端形成整改工单)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EvaluationService {

    private final EntEvaluationMapper evaluationMapper;
    private final EntApplicationRecordMapper recordMapper;
    private final EntMatterInfoMapper matterMapper;
    private final SupervisionClient supervisionClient;

    /**
     * 提交好差评(每个申报记录仅一次)
     */
    @Transactional
    public EvaluationVO submit(LoginUser user, EvaluationSubmitReq req) {
        EntApplicationRecord record = recordMapper.selectById(req.getRecordId());
        if (record == null || !record.getEnterpriseId().equals(user.getEnterpriseId())) {
            throw new BusinessException(403, "申报记录不存在或无权评价");
        }
        if ("DRAFT".equals(record.getStatus())) {
            throw new BusinessException("草稿尚未提交,暂不能评价");
        }
        Long exists = evaluationMapper.selectCount(new LambdaQueryWrapper<EntEvaluation>()
                .eq(EntEvaluation::getRecordId, req.getRecordId()));
        if (exists != null && exists > 0) {
            throw new BusinessException("该申报已评价过,不可重复评价");
        }
        EntEvaluation e = new EntEvaluation();
        e.setRecordId(req.getRecordId());
        e.setEnterpriseId(user.getEnterpriseId());
        e.setUserId(user.getUserId());
        e.setStarRating(req.getStarRating());
        e.setContent(req.getContent());
        e.setTags(req.getTags() == null ? null : JSONUtil.toJsonStr(req.getTags()));
        evaluationMapper.insert(e);

        // 差评(1-2 星)上报监管端,生成"差评整改工单" —— 好差评整改闭环;
        // 上报失败只记日志,不影响评价本身已提交(容错,不阻断主流程)
        if (req.getStarRating() != null && req.getStarRating() <= 2) {
            try {
                EntMatterInfo m = matterMapper.selectById(record.getMatterId());
                java.util.Map<String, Object> body = new java.util.HashMap<>();
                body.put("source", "ENTERPRISE");
                body.put("applicantName", user.getRealName());
                body.put("matterName", m == null ? null : m.getMatterName());
                body.put("recordCode", record.getRecordCode());
                body.put("starRating", req.getStarRating());
                body.put("content", req.getContent());
                body.put("tags", req.getTags() == null ? null : String.join(",", req.getTags()));
                supervisionClient.reportReviewOrder(body);
                log.info("差评已上报监管端生成整改工单 recordId={} star={}", record.getId(), req.getStarRating());
            } catch (Exception ex) {
                log.warn("差评上报监管端失败(忽略,不影响评价提交): {}", ex.getMessage());
            }
        }
        return toVO(e, record);
    }

    /**
     * 查询某申报记录的评价(未评价返回 null)
     */
    public EvaluationVO getByRecord(LoginUser user, Long recordId) {
        EntApplicationRecord record = recordMapper.selectById(recordId);
        if (record == null || !record.getEnterpriseId().equals(user.getEnterpriseId())) {
            throw new BusinessException(403, "申报记录不存在或无权查看");
        }
        EntEvaluation e = evaluationMapper.selectOne(new LambdaQueryWrapper<EntEvaluation>()
                .eq(EntEvaluation::getRecordId, recordId)
                .last("LIMIT 1"));
        return e == null ? null : toVO(e, record);
    }

    /**
     * 我的评价分页列表
     */
    public PageResult<EvaluationVO> pageMine(LoginUser user, int page, int size) {
        Page<EntEvaluation> p = evaluationMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<EntEvaluation>()
                        .eq(EntEvaluation::getEnterpriseId, user.getEnterpriseId())
                        .orderByDesc(EntEvaluation::getId));
        List<EvaluationVO> list = p.getRecords().stream()
                .map(e -> toVO(e, recordMapper.selectById(e.getRecordId())))
                .collect(Collectors.toList());
        return PageResult.of(list, p.getTotal(), page, size);
    }

    private EvaluationVO toVO(EntEvaluation e, EntApplicationRecord record) {
        EvaluationVO vo = new EvaluationVO();
        vo.setId(e.getId());
        vo.setRecordId(e.getRecordId());
        vo.setRecordCode(record == null ? null : record.getRecordCode());
        if (record != null && record.getMatterId() != null) {
            EntMatterInfo m = matterMapper.selectById(record.getMatterId());
            vo.setMatterName(m == null ? null : m.getMatterName());
        }
        vo.setStarRating(e.getStarRating());
        vo.setContent(e.getContent());
        if (e.getTags() != null && !e.getTags().isBlank()) {
            try {
                vo.setTags(JSONUtil.toList(e.getTags(), String.class));
            } catch (Exception ignored) {
                vo.setTags(null);
            }
        }
        vo.setCreateTime(e.getCreateTime());
        return vo;
    }
}
