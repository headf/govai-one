package com.govai.approval.service;

import com.govai.approval.common.Constants;
import com.govai.approval.dto.ApprovalRecordPushDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 审批数据推送生产者
 *
 * 接口9：审批端 → 监管端（通过 RabbitMQ 推送）
 *   交换机:   approval.exchange
 *   路由键:   approval.record.push
 *   队列:     approval.record.queue
 *
 * 监管端监听 approval.record.queue 后写入 approval_data_cache 表
 *
 * 当 RabbitMQ 不可用时降级为日志输出，不阻塞审批主流程
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ApprovalMqProducer {

    private final RabbitTemplate rabbitTemplate;

    @Value("${mq.approval.exchange:" + Constants.MQ_EXCHANGE + "}")
    private String exchange;

    @Value("${mq.approval.routing-key:" + Constants.MQ_ROUTING_KEY + "}")
    private String routingKey;

    public void pushApprovalRecord(ApprovalRecordPushDTO dto) {
        try {
            rabbitTemplate.convertAndSend(exchange, routingKey, dto);
            log.info("推送审批数据到监管端 instanceId={} status={}",
                    dto.getInstanceId(), dto.getApprovalStatus());
        } catch (Exception e) {
            // 降级：审批主流程不被 MQ 故障阻塞
            log.warn("RabbitMQ 推送失败，降级记录日志。instanceId={} err={}",
                    dto.getInstanceId(), e.getMessage());
        }
    }
}
