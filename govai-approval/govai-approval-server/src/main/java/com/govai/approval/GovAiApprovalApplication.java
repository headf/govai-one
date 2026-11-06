package com.govai.approval;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.mybatis.spring.annotation.MapperScan;

/**
 * GovAI One 审批员端服务启动类
 * 端口：8082
 *
 * 三大核心能力：
 * 1. 流程引擎（流程模板 + 实例 + 任务 + 流转）
 * 2. 智能审批（规则引擎自动审核 + AI辅助研判）
 * 3. AI能力中台对接（LangChain4j 大模型 + RabbitMQ 推送监管端）
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients
@EnableRabbit
@EnableScheduling
@MapperScan("com.govai.approval.mapper")
public class GovAiApprovalApplication {
    public static void main(String[] args) {
        SpringApplication.run(GovAiApprovalApplication.class, args);
    }
}
