package com.govai.supervision.mq;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.govai.supervision.config.RabbitConfig;
import com.govai.supervision.entity.ApprovalDataCache;
import com.govai.supervision.mapper.ApprovalDataCacheMapper;
import com.govai.supervision.ws.WebSocketBroadcaster;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 审批数据接收消费者（契约 9）
 * 监听 approval.record.queue，写入 approval_data_cache，并向大屏 WebSocket 广播
 * 手动确认：处理成功后回执消息，保证审批数据不丢失；异常时 reject 重试/丢弃
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ApprovalDataConsumer {

    private final ApprovalDataCacheMapper approvalDataCacheMapper;
    private final WebSocketBroadcaster webSocketBroadcaster;

    @RabbitListener(queues = RabbitConfig.QUEUE)
    public void onMessage(ApprovalPushMessage msg,
                          Channel channel,
                          @Header(AmqpHeaders.DELIVERY_TAG) long tag) throws IOException {
        try {
            persist(msg);
            webSocketBroadcaster.broadcast(Map.of("event", "approval.updated", "instanceId", msg.getInstanceId()));
            channel.basicAck(tag, false);
        } catch (Exception e) {
            log.error("审批数据落库失败，instanceId={}", msg == null ? null : msg.getInstanceId(), e);
            // 业务处理失败：拒绝并丢弃（requeue=false，避免死循环卡死队列）
            channel.basicReject(tag, false);
        }
    }

    /**
     * 写入 approval_data_cache，按 instanceId 去重（同一办件后续更新覆盖旧记录）
     */
    @Transactional
    public void persist(ApprovalPushMessage message) {
        if (message == null || message.getInstanceId() == null) {
            log.warn("收到空或无 instanceId 的审批消息，忽略");
            return;
        }
        ApprovalDataCache cache = approvalDataCacheMapper.selectOne(
                new LambdaQueryWrapper<ApprovalDataCache>()
                        .eq(ApprovalDataCache::getInstanceId, message.getInstanceId()));
        if (cache == null) {
            cache = new ApprovalDataCache();
            cache.setInstanceId(message.getInstanceId());
            cache.setReceiveTime(LocalDateTime.now());
        }
        cache.setMatterName(message.getMatterName());
        cache.setApplicantName(message.getApplicantName());
        cache.setApplicantType(message.getApplicantType());
        cache.setApprovalStatus(message.getApprovalStatus());
        cache.setSubmitTime(message.getSubmitTime());
        cache.setFinishTime(message.getFinishTime());
        cache.setProcessDurationHours(message.getProcessDurationHours());
        cache.setEnterpriseId(message.getEnterpriseId());
        cache.setEnterpriseName(message.getEnterpriseName());
        cache.setApplicantUserId(message.getApplicantUserId());
        cache.setApplicantPhone(message.getApplicantPhone());
        cache.setApplicantIdCard(message.getApplicantIdCard());
        if (cache.getId() == null) {
            approvalDataCacheMapper.insert(cache);
        } else {
            approvalDataCacheMapper.updateById(cache);
        }
        log.info("审批数据已入库 instanceId={}, status={}", message.getInstanceId(), message.getApprovalStatus());
    }
}