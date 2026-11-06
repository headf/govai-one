package com.govai.supervision;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.mybatis.spring.annotation.MapperScan;

/**
 * 监管端启动类
 * GovAI One 智慧政务系统 - govai-supervision
 */
@EnableScheduling
@SpringBootApplication
@MapperScan("com.govai.supervision.mapper")
public class GovAiSupervisionApplication {

    public static void main(String[] args) {
        SpringApplication.run(GovAiSupervisionApplication.class, args);
    }

}