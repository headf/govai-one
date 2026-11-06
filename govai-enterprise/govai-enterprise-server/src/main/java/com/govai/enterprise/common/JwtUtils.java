package com.govai.enterprise.common;

import cn.hutool.core.util.StrUtil;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT 工具(与全组共享同一密钥,骨架文档 6)
 * Payload: userId / userName / role / enterpriseId
 */
@Component
public class JwtUtils {

    @Value("${govai.jwt.secret}")
    private String secret;

    @Value("${govai.jwt.expire}")
    private long expire;

    private SecretKey key() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 生成 Token
     *
     * @param userId       用户ID
     * @param userName     用户名(登录账号)
     * @param realName     真实姓名
     * @param role         ADMIN/OPERATOR(企业端内部角色)
     * @param enterpriseId 所属企业ID
     */
    public String generateToken(Long userId, String userName, String realName,
                                String role, Long enterpriseId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("userName", userName);
        claims.put("realName", realName);
        claims.put("role", role);
        claims.put("enterpriseId", enterpriseId);
        claims.put("loginType", "ENTERPRISE");
        Date now = new Date();
        return Jwts.builder()
                .claims(claims)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expire * 1000))
                .signWith(key())
                .compact();
    }

    /**
     * 解析 Token,失败返回 null
     */
    public Claims parse(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(key())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (Exception e) {
            return null;
        }
    }

    public boolean validate(String token) {
        return StrUtil.isNotBlank(token) && parse(token) != null;
    }

    public String getSecret() {
        return secret;
    }
}
