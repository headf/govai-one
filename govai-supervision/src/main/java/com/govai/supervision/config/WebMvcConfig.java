package com.govai.supervision.config;

import com.govai.supervision.interceptor.JwtInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * WebMvc 配置
 * 注册 JWT 拦截器，排除登录与文档相关路径
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final JwtInterceptor jwtInterceptor;

    @Override
    public void addInterceptors(@NonNull InterceptorRegistry registry) {
        registry.addInterceptor(jwtInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                        // SSO 登录（契约1）与短信快捷登录（公民端）；verify 需带 token
                        "/api/v1/auth/login",
                        "/api/v1/auth/sms-login",
                        // WebSocket 大屏连接（MQ 触发推送，无 HTTP Header）
                        "/ws", "/ws/**",
                        // 企业端视角接口（ent-view，企业端 Feign/前端带 X-Enterprise-Id 访问，不走监管端用户 JWT）
                        "/api/v1/ent-view/**",
                        // 公民(群众)端视角接口（cit-view，群众端上报办件/查信用，身份由请求自证）
                        "/api/v1/cit-view/**",
                        // 差评上报（群众端/企业端提交差评时 Feign 上报，身份由上报方自证）
                        "/api/v1/review-orders/report",
                        // Knife4j / Swagger 文档
                        "/doc.html",
                        "/swagger-ui/**",
                        "/swagger-ui.html",
                        "/webjars/**",
                        "/v3/api-docs/**",
                        "/v3/api-docs.yaml",
                        "/favicon.ico",
                        "/error"
                );
    }
}