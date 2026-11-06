package com.govai.approval.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Knife4j / OpenAPI 接口文档配置
 */
@Configuration
public class Knife4jConfig {

    @Bean
    public OpenAPI approvalOpenAPI() {
        return new OpenAPI().info(new Info()
                .title("GovAI One 审批员端接口文档")
                .description("智能审批模块 - 流程引擎 + 规则引擎 + AI辅助研判")
                .version("v1.0.0")
                .contact(new Contact().name("张可").email("zhangke@govai.com"))
                .license(new License().name("Apache 2.0").url("https://www.apache.org/licenses/LICENSE-2.0")));
    }
}
