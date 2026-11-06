package com.govai.supervision.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Knife4j / OpenAPI 文档配置
 * 访问地址：http://localhost:8081/doc.html
 */
@Configuration
public class Knife4jConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("govai-supervision 监管端 API")
                        .description("GovAI One 智慧政务系统 - 监管端（SSO 认证 / 信用评价 / 风险预警 / 统计大屏）")
                        .version("1.0.0")
                        .contact(new Contact().name("杨星宇").email("govai@example.com")));
    }
}