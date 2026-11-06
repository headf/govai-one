package com.govai.citizen.service;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.govai.citizen.common.BizException;
import com.govai.citizen.common.Constants;
import com.govai.citizen.common.PageResult;
import com.govai.citizen.config.GovaiProperties;
import com.govai.citizen.config.UserContext;
import com.govai.citizen.dto.ApplyResultVO;
import com.govai.citizen.dto.ApplySubmitReq;
import com.govai.citizen.dto.RecordDetailVO;
import com.govai.citizen.dto.RecordListVO;
import com.govai.citizen.dto.RecordQuery;
import com.govai.citizen.entity.ApplicationMaterial;
import com.govai.citizen.entity.ApplicationRecord;
import com.govai.citizen.entity.CitizenUser;
import com.govai.citizen.entity.MatterInfo;
import com.govai.citizen.entity.ProgressNotification;
import com.govai.citizen.entity.SiteMessage;
import com.govai.citizen.feign.ApprovalClient;
import com.govai.citizen.feign.SupervisionClient;
import com.govai.citizen.mapper.ApplicationMaterialMapper;
import com.govai.citizen.mapper.ApplicationRecordMapper;
import com.govai.citizen.mapper.CitizenUserMapper;
import com.govai.citizen.mapper.MatterInfoMapper;
import com.govai.citizen.mapper.ProgressNotificationMapper;
import com.govai.citizen.mapper.SiteMessageMapper;
import com.govai.citizen.util.JsonUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ApplyService {

    private final ApplicationRecordMapper recordMapper;
    private final ApplicationMaterialMapper materialMapper;
    private final MatterInfoMapper matterMapper;
    private final SiteMessageMapper messageMapper;
    private final ProgressNotificationMapper notificationMapper;
    private final ApprovalClient approvalClient;
    private final SupervisionClient supervisionClient;
    private final CitizenUserMapper citizenUserMapper;
    private final GovaiProperties props;

    @Transactional(rollbackFor = Exception.class)
    public ApplyResultVO submit(ApplySubmitReq req) {
        Long uid = UserContext.requireUserId();
        MatterInfo matter = matterMapper.selectById(req.getMatterId());
        if (matter == null || matter.getStatus() == null || matter.getStatus() != 1) {
            throw new BizException("该事项不存在或已下线");
        }

        ApplicationRecord record = new ApplicationRecord();
        record.setRecordCode(genRecordCode());
        record.setMatterId(matter.getId());
        record.setUserId(uid);
        record.setApplicantName(StrUtil.blankToDefault(req.getApplicantName(), null));
        record.setApplicantPhone(StrUtil.blankToDefault(req.getApplicantPhone(), null));
        record.setApplicantIdCard(StrUtil.blankToDefault(req.getApplicantIdCard(), null));
        record.setFormData(JsonUtil.fromMap(req.getFormData()));
        record.setStatus(Constants.STATUS_PENDING);
        record.setSubmitTime(LocalDateTime.now());
        recordMapper.insert(record);

        if (req.getMaterials() != null) {
            for (ApplySubmitReq.MaterialUploadItem item : req.getMaterials()) {
                ApplicationMaterial am = new ApplicationMaterial();
                am.setRecordId(record.getId());
                am.setMaterialName(item.getMaterialName());
                am.setFileUrl(item.getFileUrl());
                am.setFileType(item.getFileType());
                am.setOcrStatus(0);
                materialMapper.insert(am);
            }
        }

        Long instanceId = null;
        String status = Constants.STATUS_APPROVING;
        Map<String, Object> approvalReq = new LinkedHashMap<>();
        approvalReq.put("templateCode", matter.getMatterCode());
        approvalReq.put("matterId", matter.getId());
        approvalReq.put("applicantName", record.getApplicantName());
        approvalReq.put("applicantId", record.getApplicantIdCard());
        approvalReq.put("applicantType", "PERSONAL");
        approvalReq.put("formData", req.getFormData() == null ? new HashMap<>() : req.getFormData());
        Object resp = safeSubmit(approvalReq);
        if (resp instanceof Map<?, ?> m && m.get("instanceId") != null) {
            instanceId = Long.valueOf(String.valueOf(m.get("instanceId")));
            Object s = m.get("status");
            status = s == null ? Constants.STATUS_APPROVING : String.valueOf(s);
        }
        record.setApprovalInstanceId(instanceId);
        record.setStatus(status);
        recordMapper.updateById(record);

        // 真实提交到审批端后, 向监管端补报公民身份关联(失败静默, 不阻塞申报)
        if (instanceId != null && instanceId < 9000) {
            reportToSupervision(record, matter, instanceId);
        }

        sendSiteMessage(record, matter, "申报提交成功", "您的「" + matter.getMatterName() + "」申请已提交成功,审批编号 "
                + record.getRecordCode() + ",请耐心等待审批结果。");
        return buildResult(record);
    }

    /**
     * 开发阶段审批端不在线: Feign 调用失败时返回本地 Mock 审批结果,
     * 保证单机可完整演示申报→进度全流程(联调阶段审批端上线后自动走真实实例)。
     */
    private Object safeSubmit(Map<String, Object> req) {
        try {
            var resp = approvalClient.submit(req);
            if (resp != null && resp.getData() != null) {
                return resp.getData();
            }
        } catch (Exception ignored) {
            // fall through to below
        }
        // 真实联调模式(SSO 已接): 审批端不可达必须明确报错, 不静默 mock 造成"假贯通"
        if (!props.isMockLogin()) {
            throw new BizException("审批中心暂不可用，申报未能提交，请稍后重试");
        }
        Map<String, Object> mock = new LinkedHashMap<>();
        mock.put("instanceId", 9000L + Math.floorMod(System.currentTimeMillis(), 1000));
        mock.put("status", Constants.STATUS_APPROVING);
        return mock;
    }

    /**
     * 审批真实打通后, 向监管端补报办件(含公民 sys_user.id / 手机号 / 身份证), 落监管审批台账供个人画像
     */
    private void reportToSupervision(ApplicationRecord record, MatterInfo matter, Long instanceId) {
        try {
            CitizenUser user = citizenUserMapper.selectById(record.getUserId());
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("instanceId", instanceId);
            body.put("matterName", matter == null ? null : matter.getMatterName());
            body.put("applicantName", record.getApplicantName());
            body.put("applicantType", "PERSONAL");
            body.put("approvalStatus", record.getStatus());
            if (record.getSubmitTime() != null) {
                body.put("submitTime", record.getSubmitTime().toString());
            }
            if (user != null && user.getSysUserId() != null) {
                body.put("applicantUserId", user.getSysUserId());
            }
            body.put("applicantPhone", record.getApplicantPhone());
            body.put("applicantIdCard", record.getApplicantIdCard());
            try {
                supervisionClient.reportApproval(body);
            } catch (Exception first) {
                // 失败立即重试一次(应对对端重启/瞬时抖动)
                log.warn("上报监管端失败,立即重试一次 err={}", first.getMessage());
                supervisionClient.reportApproval(body);
            }
        } catch (Exception e) {
            // 上报是增强行为,不中断申报主流程;但失败必须可见
            // (原空 catch 完全静默,会造成"群众端有申报、监管大屏不更新"且无迹可查)
            log.warn("上报监管端失败(已重试一次,监管大屏可能缺此条) err={}", e.getMessage());
        }
    }

    public PageResult<RecordListVO> myRecords(RecordQuery query) {
        Long uid = UserContext.requireUserId();
        int page = query.getPage() == null || query.getPage() < 1 ? 1 : query.getPage();
        int size = query.getSize() == null || query.getSize() < 1 ? 10 : Math.min(query.getSize(), 50);
        LambdaQueryWrapper<ApplicationRecord> qw = new LambdaQueryWrapper<ApplicationRecord>()
                .eq(ApplicationRecord::getUserId, uid)
                .eq(StrUtil.isNotBlank(query.getStatus()), ApplicationRecord::getStatus, query.getStatus())
                .orderByDesc(ApplicationRecord::getSubmitTime);
        IPage<ApplicationRecord> p = recordMapper.selectPage(new Page<>(page, size), qw);
        Map<Long, String> matterNames = p.getRecords().stream()
                .map(ApplicationRecord::getMatterId)
                .distinct()
                .collect(Collectors.toMap(id -> id, id -> {
                    MatterInfo m = matterMapper.selectById(id);
                    return m == null ? "" : m.getMatterName();
                }));
        List<RecordListVO> list = p.getRecords().stream().map(r -> {
            RecordListVO vo = new RecordListVO();
            vo.setId(r.getId());
            vo.setRecordCode(r.getRecordCode());
            vo.setMatterId(r.getMatterId());
            vo.setMatterName(matterNames.getOrDefault(r.getMatterId(), ""));
            vo.setStatus(r.getStatus());
            vo.setStatusText(statusText(r.getStatus()));
            vo.setSubmitTime(r.getSubmitTime());
            vo.setFinishTime(r.getFinishTime());
            vo.setApprovalInstanceId(r.getApprovalInstanceId());
            return vo;
        }).collect(Collectors.toList());
        return new PageResult<>(list, p.getTotal(), page, size);
    }

    public RecordDetailVO detail(Long id) {
        Long uid = UserContext.requireUserId();
        ApplicationRecord r = recordMapper.selectById(id);
        if (r == null || !r.getUserId().equals(uid)) {
            throw new BizException(404, "申报记录不存在");
        }
        MatterInfo m = matterMapper.selectById(r.getMatterId());
        RecordDetailVO vo = new RecordDetailVO();
        vo.setId(r.getId());
        vo.setRecordCode(r.getRecordCode());
        vo.setMatterId(r.getMatterId());
        vo.setMatterName(m == null ? "" : m.getMatterName());
        vo.setMatterCode(m == null ? "" : m.getMatterCode());
        vo.setDepartment(m == null ? "" : m.getDepartment());
        vo.setStatus(r.getStatus());
        vo.setStatusText(statusText(r.getStatus()));
        vo.setApplicantName(r.getApplicantName());
        vo.setApplicantPhone(r.getApplicantPhone());
        vo.setApplicantIdCard(r.getApplicantIdCard());
        vo.setFormData(JsonUtil.toMap(r.getFormData()));
        vo.setApprovalInstanceId(r.getApprovalInstanceId());
        vo.setSubmitTime(r.getSubmitTime() == null ? null : r.getSubmitTime().toString());
        vo.setFinishTime(r.getFinishTime() == null ? null : r.getFinishTime().toString());
        vo.setMaterials(materialMapper.selectList(new LambdaQueryWrapper<ApplicationMaterial>()
                .eq(ApplicationMaterial::getRecordId, r.getId())
                .orderByAsc(ApplicationMaterial::getUploadTime)));
        return vo;
    }

    public void sendSiteMessage(ApplicationRecord record, MatterInfo matter, String title, String content) {
        sendSiteMessage(record, matter, title, content, "提交");
    }

    public void sendSiteMessage(ApplicationRecord record, MatterInfo matter, String title, String content, String nodeName) {
        SiteMessage msg = new SiteMessage();
        msg.setUserId(record.getUserId());
        msg.setMessageType(Constants.MSG_APPROVAL);
        msg.setTitle(title);
        msg.setContent(content);
        msg.setRecordId(record.getId());
        msg.setIsRead(0);
        messageMapper.insert(msg);

        ProgressNotification n = new ProgressNotification();
        n.setRecordId(record.getId());
        n.setNodeName(nodeName);
        n.setContent(content);
        n.setNotifyType(Constants.NOTIFY_SITE);
        n.setSendStatus(1);
        n.setSendTime(LocalDateTime.now());
        notificationMapper.insert(n);
    }

    /**
     * 开发期模拟审批通过(审批端不在线时演示完整闭环):
     * 将办件置为已办结并生成站内信 + 进度通知。
     */
    @Transactional(rollbackFor = Exception.class)
    public void mockApprove(Long recordId) {
        Long uid = UserContext.requireUserId();
        ApplicationRecord r = recordMapper.selectById(recordId);
        if (r == null || !r.getUserId().equals(uid)) {
            throw new BizException(404, "申报记录不存在");
        }
        if (Constants.STATUS_APPROVED.equals(r.getStatus())) {
            throw new BizException("该办件已是办结状态");
        }
        if (r.getApprovalInstanceId() != null) {
            throw new BizException("该办件已在审批端真实流转，请前往审批中心办理");
        }
        MatterInfo matter = matterMapper.selectById(r.getMatterId());
        r.setStatus(Constants.STATUS_APPROVED);
        r.setFinishTime(LocalDateTime.now());
        recordMapper.updateById(r);

        String title = "「" + (matter == null ? "" : matter.getMatterName()) + "」已办结";
        String content = "您的申报编号 " + r.getRecordCode() + " 已审批通过并办结,可前往评价办件。感谢您的使用!";
        sendSiteMessage(r, matter, title, content, "办结");
    }

    /**
     * 撤回申报: 仅本地待受理(尚未进入审批端真实流转)的办件允许撤回
     */
    @Transactional(rollbackFor = Exception.class)
    public void withdraw(Long recordId) {
        Long uid = UserContext.requireUserId();
        ApplicationRecord r = recordMapper.selectById(recordId);
        if (r == null || !r.getUserId().equals(uid)) {
            throw new BizException(404, "申报记录不存在");
        }
        if (r.getApprovalInstanceId() != null) {
            throw new BizException("该办件已进入审批中心办理，无法撤回，如有疑问请联系审批中心");
        }
        if (!Constants.STATUS_PENDING.equals(r.getStatus())) {
            throw new BizException("仅待受理的申报可撤回");
        }
        MatterInfo matter = matterMapper.selectById(r.getMatterId());
        r.setStatus(Constants.STATUS_REJECTED);
        r.setFinishTime(LocalDateTime.now());
        recordMapper.updateById(r);
        sendSiteMessage(r, matter, "申报已撤回", "您的申报编号 " + r.getRecordCode() + " 已撤回，如需办理请重新提交。", "撤回");
    }

    private ApplyResultVO buildResult(ApplicationRecord record) {
        ApplyResultVO vo = new ApplyResultVO();
        vo.setRecordId(record.getId());
        vo.setRecordCode(record.getRecordCode());
        vo.setApprovalInstanceId(record.getApprovalInstanceId());
        vo.setStatus(record.getStatus());
        return vo;
    }

    private String genRecordCode() {
        return "CZ" + IdUtil.getSnowflakeNextIdStr();
    }

    private String statusText(String status) {
        if (Constants.STATUS_APPROVING.equals(status)) {
            return "审批中";
        }
        if (Constants.STATUS_APPROVED.equals(status)) {
            return "已办结";
        }
        if (Constants.STATUS_REJECTED.equals(status)) {
            return "已驳回";
        }
        return "受理中";
    }
}
