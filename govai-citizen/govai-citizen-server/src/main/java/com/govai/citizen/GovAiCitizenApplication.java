package com.govai.citizen;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@MapperScan("com.govai.citizen.mapper")
@EnableFeignClients(basePackages = "com.govai.citizen.feign")
public class GovAiCitizenApplication {

    public static void main(String[] args) {
        SpringApplication.run(GovAiCitizenApplication.class, args);
    }
}
