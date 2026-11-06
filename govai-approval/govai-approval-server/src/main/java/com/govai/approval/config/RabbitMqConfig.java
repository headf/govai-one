package com.govai.approval.config;

import com.govai.approval.common.Constants;
import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ 配置
 *
 * 审批端只负责"发送"，但声明 Exchange/Queue 是幂等的，方便启动时自动创建
 * 监管端启动后也会声明同名 Queue，自动绑定
 */
@Configuration
public class RabbitMqConfig {

    public static final String EXCHANGE    = Constants.MQ_EXCHANGE;
    public static final String ROUTING_KEY = Constants.MQ_ROUTING_KEY;
    public static final String QUEUE       = Constants.MQ_QUEUE;

    @Bean
    public DirectExchange approvalExchange() {
        return ExchangeBuilder.directExchange(EXCHANGE).durable(true).build();
    }

    @Bean
    public Queue approvalQueue() {
        return QueueBuilder.durable(QUEUE).build();
    }

    @Bean
    public Binding approvalBinding(Queue approvalQueue, DirectExchange approvalExchange) {
        return BindingBuilder.bind(approvalQueue).to(approvalExchange).with(ROUTING_KEY);
    }

    /** 用 JSON 序列化消息，便于监管端反序列化为 DTO */
    @Bean
    public MessageConverter rabbitMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
