package com.govai.supervision.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT 工具类（SSO 认证核心）
 * 全组共享同一密钥签发，其他端本地验证，开发阶段无需调用监管端在线
 *
 * Payload 结构：userId / userName / role
 */
@Slf4j
@Component
public class JwtUtils {

    /** JWT 共享密钥（全组必须一致） */
    private final String secret;

    /** 过期时间（秒），默认 7 天 */
    private final long expire;

    public JwtUtils(@Value("${govai.jwt.secret}") String secret,
                    @Value("${govai.jwt.expire:604800}") long expire) {
        this.secret = secret;
        this.expire = expire;
    }

    /** 由密钥生成 SecretKey（HS256 ≥ 256bit，本密钥长度满足） */
    private SecretKey getKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 生成 Token
     *
     * @param userId   用户ID
     * @param userName 用户名
     * @param role     角色编码（CITIZEN/ENTERPRISE/APPROVER/SUPERVISOR/ADMIN）
     */
    public String generateToken(Long userId, String userName, String role) {
        return generateToken(userId, userName, role, null);
    }

    /**
     * 生成 Token（扩展：额外携带业务 claim，如企业端登录需带 enterpriseId）
     */
    public String generateToken(Long userId, String userName, String role, Map<String, Object> extraClaims) {
        Map<String, Object> claims = new HashMap<>(8);
        claims.put("userId", userId);
        claims.put("userName", userName);
        claims.put("role", role);
        if (extraClaims != null) {
            claims.putAll(extraClaims);
        }

        Date now = new Date();
        Date expiry = new Date(now.getTime() + expire * 1000L);

        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(now)
                .setExpiration(expiry)
                .signWith(getKey())
                .compact();
    }

    /**
     * 解析 Token，返回 Claims
     * 若 Token 无效或过期，返回 null
     */
    public Claims parseToken(String token) {
        try {
            return Jwts.parser()
                    .setSigningKey(getKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (Exception e) {
            log.warn("JWT 解析失败: {}", e.getMessage());
            return null;
        }
    }

    /** 从 Token 解析 userId */
    public Long getUserId(String token) {
        Claims claims = parseToken(token);
        if (claims == null) {
            return null;
        }
        Object v = claims.get("userId");
        return v == null ? null : Long.valueOf(v.toString());
    }
}