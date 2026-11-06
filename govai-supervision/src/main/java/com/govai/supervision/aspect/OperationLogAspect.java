package com.govai.supervision.aspect;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.govai.supervision.entity.SysLog;
import com.govai.supervision.interceptor.JwtInterceptor;
import com.govai.supervision.service.SysLogService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * 操作日志切面
 * 记录 controller 写操作（POST/PUT/DELETE）到 sys_log。
 * 复用 JwtInterceptor 放入 request 的 loginUserId / loginUserName。
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class OperationLogAspect {

    private final SysLogService sysLogService;
    private final ObjectMapper objectMapper;

    @Pointcut("execution(* com.govai.supervision.controller..*(..))")
    public void controllerMethods() {
    }

    @Around("controllerMethods() && @annotation(org.springframework.web.bind.annotation.PostMapping)"
            + "|| controllerMethods() && @annotation(org.springframework.web.bind.annotation.PutMapping)"
            + "|| controllerMethods() && @annotation(org.springframework.web.bind.annotation.DeleteMapping)")
    public Object aroundWrite(ProceedingJoinPoint pjp) throws Throwable {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = attrs != null ? attrs.getRequest() : null;
        String uri = request != null ? request.getRequestURI() : "";

        // 登录/登出与日志自身操作不记日志，避免刷屏与递归
        if (uri.contains("/auth/login") || uri.startsWith("/api/v1/logs") || uri.contains("/ws")) {
            return pjp.proceed();
        }

        long start = System.currentTimeMillis();
        Object result = pjp.proceed();
        long cost = System.currentTimeMillis() - start;

        try {
            if (request != null) {
                SysLog sysLog = new SysLog();
                Object uid = request.getAttribute(JwtInterceptor.ATTR_USER_ID);
                Object uname = request.getAttribute(JwtInterceptor.ATTR_USER_NAME);
                sysLog.setUserId(uid == null ? null : Long.valueOf(uid.toString()));
                sysLog.setUsername(uname == null ? null : uname.toString());
                sysLog.setMethod(request.getMethod() + " " + uri);
                sysLog.setOperation(resolveOperation(request.getMethod(), uri));
                sysLog.setParams(buildParams(pjp.getArgs()));
                sysLog.setIp(resolveIp(request));
                sysLog.setCostTime(cost);
                sysLog.setCreateTime(LocalDateTime.now());
                sysLogService.record(sysLog);
            }
        } catch (Exception e) {
            log.warn("记录操作日志失败 uri={}", uri, e);
        }
        return result;
    }

    /** 生成操作描述 */
    private String resolveOperation(String httpMethod, String uri) {
        String path = uri.replace("/api/v1", "").replace("/{", "/").replace("}", "");
        String[] seg = path.split("/");
        if (seg.length >= 2) {
            String module = seg[1];
            return switch (httpMethod) {
                case "POST" -> module + (seg.length > 2 ? "-" + seg[2] : "-新增/操作");
                case "PUT" -> module + "-更新";
                case "DELETE" -> module + "-删除";
                default -> module;
            };
        }
        return httpMethod + " " + uri;
    }

    /** 序列化请求参数（忽略 MultipartFile/流） */
    private String buildParams(Object[] args) {
        try {
            String params = Arrays.stream(args)
                    .filter(a -> !(a instanceof MultipartFile) && a != null
                            && !isServletType(a))
                    .map(a -> {
                        try {
                            return objectMapper.writeValueAsString(a);
                        } catch (Exception e) {
                            return a.toString();
                        }
                    })
                    .collect(Collectors.joining(", "));
            if (params.length() > 2000) {
                params = params.substring(0, 2000);
            }
            return params;
        } catch (Exception e) {
            return "";
        }
    }

    private boolean isServletType(Object o) {
        return o instanceof jakarta.servlet.ServletRequest || o instanceof jakarta.servlet.ServletResponse
                || o instanceof jakarta.servlet.http.HttpSession;
    }

    private String resolveIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip != null && ip.contains(",") ? ip.split(",")[0].trim() : ip;
    }
}
