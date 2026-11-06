package com.govai.enterprise.config;

import com.govai.enterprise.interceptor.JwtAuthInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 配置:注册 JWT 拦截器 + 跨域 + 静态资源
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final JwtAuthInterceptor jwtAuthInterceptor;

    /** 免登录白名单 */
    private static final String[] EXCLUDE_PATHS = {
            "/api/v1/ent-auth/**",      // 登录/注册(企业认证中心)
            "/api/v1/sys/health",       // 健康检查
            "/api/v1/ent-matters/list",       // 事项大厅列表(游客可浏览)
            "/api/v1/ent-matters/categories", // 事项分类(游客可浏览)
            // 事项详情 /ent-matters/{id} 与智能规划 /ent-matters/plan 同为单层路径,
            // /* 通配符会同时匹配两者导致 plan 接口免登录但 Controller 需要登录上下文而抛 401,
            // 故移除该通配符,事项详情/智能规划均需登录访问(符合企业端定位)
            "/api/v1/ocr/**",           // OCR 对外能力接口(审批端以 API 密钥调用,暂放行)
            "/api/v1/ent-apply/*/approval-result", // 审批结果回调(审批端推送,服务间调用)
            "/api/v1/ent-apply/approval-result-by-instance", // 审批定稿回调(按审批实例,审批端推送,服务间调用)
            "/api/v1/ent-apply/materials-by-instance/**", // 按实例查申报材料(审批端复核材料,服务间调用)
            "/api/v1/ent-enterprise/batch", // 企业摘要批量查询(监管端等跨服务只读调用)
            "/api/v1/ent-enterprise/users/members", // 企业法人+成员查询(监管端等跨服务只读调用)
            "/api/v1/ent-enterprise/users/all", // 全部企业用户(监管端用户管理展示,跨服只读)
            "/api/v1/ent-enterprise/users/members/*/disable", // 监管端跨服禁用经办人(不含法人路径 /{id}/disable)
            "/api/v1/ent-enterprise/users/members/*/enable", // 监管端跨服启用经办人
            "/doc.html", "/webjars/**", "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html",
            "/favicon.ico", "/error"
    };

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwtAuthInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns(EXCLUDE_PATHS);
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }
}
