package com.govai.approval.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * JWT 配置
 * 4人共享密钥：通过环境变量 JWT_SECRET 注入
 */
@Data
@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {
    /** 共享密钥（与监管端、群众端、企业端完全一致） */
    private String secret;
    /** 过期时间（秒） */
    private long expire;
    /** 请求头名称 */
    private String header;
    /** Token 前缀 */
    private String prefix;
}
