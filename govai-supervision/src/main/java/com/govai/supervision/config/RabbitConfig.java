package com.govai.supervision.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ 交换机/队列/绑定 声明
 * 与审批端约定（契约 9，文档 5.9）：
 *   交换机: approval.exchange       类型 direct
 *   路由键: approval.record.push
 *   队列:   approval.record.queue
 * 审批端推送审批结果 → 本端消费者写入 approval_data_cache 表
 */
@Configuration
public class RabbitConfig {

    public static final String EXCHANGE = "approval.exchange";
    public static final String ROUTING_KEY = "approval.record.push";
    public static final String QUEUE = "approval.record.queue";

    @Bean
    public DirectExchange approvalExchange() {
        return new DirectExchange(EXCHANGE, true, false);
    }

    @Bean
    public Queue approvalQueue() {
        return new Queue(QUEUE, true);
    }

    @Bean
    public Binding approvalBinding(DirectExchange approvalExchange, Queue approvalQueue) {
        return BindingBuilder.bind(approvalQueue).to(approvalExchange).with(ROUTING_KEY);
    }

    /**
     * JSON 消息转换器：接收审批端以 JSON 结构体推送的审批数据，自动反序列化为 DTO
     */
    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter messageConverter) {
        RabbitTemplate rabbit = new RabbitTemplate(connectionFactory);
        rabbit.setMessageConverter(messageConverter);
        return rabbit;
    }
}