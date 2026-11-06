package com.govai.approval.common;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT 工具类
 * 全组共享同一密钥：通过环境变量 JWT_SECRET 注入
 * 监管端签发，本端本地验证，开发阶段不需要调监管端
 */
public final class JwtUtils {

    private JwtUtils() {}

    /**
     * 用配置密钥生成 SecretKey（HMAC-SHA 算法要求密钥≥256bit）
     */
    public static SecretKey signingKey(String secret) {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        // 密钥不足32字节则补齐，避免抛 WeakKeyException
        if (keyBytes.length < 32) {
            byte[] padded = new byte[32];
            System.arraycopy(keyBytes, 0, padded, 0, keyBytes.length);
            keyBytes = padded;
        }
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * 签发 JWT（一般由监管端签发，这里提供能力备用）
     */
    public static String sign(Long userId, String userName, String role, String secret, long expireSeconds) {
        long now = System.currentTimeMillis();
        Map<String, Object> claims = new HashMap<>();
        claims.put(Constants.CLAIM_USER_ID, userId);
        claims.put(Constants.CLAIM_USER_NAME, userName);
        claims.put(Constants.CLAIM_ROLE, role);
        return Jwts.builder()
                .claims(claims)
                .issuedAt(new Date(now))
                .expiration(new Date(now + expireSeconds * 1000))
                .signWith(signingKey(secret))
                .compact();
    }

    /**
     * 解析 JWT（本端验证 Token 是否有效）
     * @return claims  解析失败抛异常
     */
    public static Claims parse(String token, String secret) {
        Jws<Claims> jws = Jwts.parser()
                .verifyWith(signingKey(secret))
                .build()
                .parseSignedClaims(token);
        return jws.getPayload();
    }

    /**
     * 安全解析：过期/非法时返回 null，不抛异常
     */
    public static Claims parseSafely(String token, String secret) {
        try {
            return parse(token, secret);
        } catch (ExpiredJwtException e) {
            return null;
        } catch (Exception e) {
            return null;
        }
    }
}
