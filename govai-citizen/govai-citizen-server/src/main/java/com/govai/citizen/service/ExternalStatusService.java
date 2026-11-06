package com.govai.citizen.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.govai.citizen.common.Constants;
import com.govai.citizen.entity.ApplicationRecord;
import com.govai.citizen.entity.MatterInfo;
import com.govai.citizen.entity.ProgressNotification;
import com.govai.citizen.entity.SiteMessage;
import com.govai.citizen.mapper.ApplicationRecordMapper;
import com.govai.citizen.mapper.MatterInfoMapper;
import com.govai.citizen.mapper.ProgressNotificationMapper;
import com.govai.citizen.mapper.SiteMessageMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 审批端终态回调: 同步本地申报状态 + 生成 APPROVAL 站内信/进度通知(办结/驳回/退回实时提示)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExternalStatusService {

    private final ApplicationRecordMapper recordMapper;
    private final MatterInfoMapper matterMapper;
    private final SiteMessageMapper messageMapper;
    private final ProgressNotificationMapper notificationMapper;

    public void handleStatus(Map<String, Object> body) {
        if (body == null || body.get("instanceId") == null || body.get("status") == null) {
            log.warn("审批回调报文缺失 instanceId/status body={}", body);
            return;
        }
        Long instanceId = Long.valueOf(String.valueOf(body.get("instanceId")));
        String status = String.valueOf(body.get("status"));
        // 仅处理终态, 幂等(本地已是同终态则跳过, 避免重复站内信)
        if (!isTerminal(status)) {
            return;
        }
        ApplicationRecord r = recordMapper.selectOne(new LambdaQueryWrapper<ApplicationRecord>()
                .eq(ApplicationRecord::getApprovalInstanceId, instanceId)
                .last("limit 1"));
        if (r == null) {
            log.info("审批回调 instanceId={} 无对应群众申报记录, 忽略(可能是非群众端事项)", instanceId);
            return;
        }
        if (status.equals(r.getStatus())) {
            return;
        }
        r.setStatus(status);
        if (body.get("finishTime") != null) {
            try {
                r.setFinishTime(LocalDateTime.parse(String.valueOf(body.get("finishTime")).substring(0, 19)));
            } catch (Exception ignore) {
                // 时间格式异常不影响状态更新
            }
        }
        recordMapper.updateById(r);

        String title = switch (status) {
            case Constants.STATUS_APPROVED -> "办件已办结";
            case Constants.STATUS_REJECTED -> "办件未通过";
            default -> "办件已退回";
        };
        MatterInfo matter = r.getMatterId() == null ? null : matterMapper.selectById(r.getMatterId());
        String matterName = matter == null ? "该事项" : matter.getMatterName();
        String content = "您的「" + matterName + "」申报(" + r.getRecordCode() + ")已"
                + (Constants.STATUS_APPROVED.equals(status) ? "办结，可查看结果并评价。"
                : Constants.STATUS_REJECTED.equals(status) ? "未通过审批，可查看驳回意见。"
                : "退回补正，请按补正意见完善后重新提交。");
        Object opinion = body.get("opinion");
        if (!Constants.STATUS_APPROVED.equals(status) && opinion != null && String.valueOf(opinion).isBlank() == false) {
            content += "\n审批意见：" + String.valueOf(opinion);
        }
        SiteMessage msg = new SiteMessage();
        msg.setUserId(r.getUserId());
        msg.setMessageType(Constants.MSG_APPROVAL);
        msg.setTitle(title);
        msg.setContent(content);
        msg.setRecordId(r.getId());
        msg.setIsRead(0);
        messageMapper.insert(msg);

        ProgressNotification n = new ProgressNotification();
        n.setRecordId(r.getId());
        n.setNodeName(Constants.STATUS_APPROVED.equals(status) ? "办结" : "定稿");
        n.setContent(content);
        n.setNotifyType(Constants.NOTIFY_SITE);
        n.setSendStatus(1);
        n.setSendTime(LocalDateTime.now());
        notificationMapper.insert(n);
        log.info("已同步审批终态 recordId={} instanceId={} status={}", r.getId(), instanceId, status);
    }

    private boolean isTerminal(String status) {
        return Constants.STATUS_APPROVED.equals(status)
                || Constants.STATUS_REJECTED.equals(status)
                || "RETURNED".equals(status);
    }
}
