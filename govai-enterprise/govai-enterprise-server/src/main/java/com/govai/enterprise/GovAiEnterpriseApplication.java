package com.govai.enterprise;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * GovAI One 企业端服务启动类
 * 负责人:郝锦瑞 | 端口:8084 | 库:govai_enterprise
 */
@SpringBootApplication
@MapperScan("com.govai.enterprise.mapper")
@EnableFeignClients(basePackages = "com.govai.enterprise.feign")
public class GovAiEnterpriseApplication {

    public static void main(String[] args) {
        SpringApplication.run(GovAiEnterpriseApplication.class, args);
    }
}
