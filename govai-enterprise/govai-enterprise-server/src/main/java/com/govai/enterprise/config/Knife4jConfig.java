package com.govai.enterprise.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Knife4j / OpenAPI 文档配置
 * 访问地址:http://localhost:8084/doc.html
 */
@Configuration
public class Knife4jConfig {

    @Bean
    public OpenAPI govaiEnterpriseOpenApi() {
        return new OpenAPI().info(new Info()
                .title("GovAI One 企业端 API")
                .description("企业办事门户后端接口(郝锦瑞)")
                .version("v1.0")
                .contact(new Contact().name("郝锦瑞")));
    }
}
