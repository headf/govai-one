package com.govai.citizen.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class Knife4jConfig {

    @Bean
    public OpenAPI govaiOpenAPI() {
        return new OpenAPI().info(new Info()
                .title("GovAI One 群众端 API")
                .description("智慧政务系统 - 群众端(govai-citizen) 接口文档")
                .version("1.0.0"));
    }
}
