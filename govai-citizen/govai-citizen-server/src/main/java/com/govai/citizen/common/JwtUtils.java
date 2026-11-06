package com.govai.citizen.common;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 工具: 与全组共享密钥一致, 本地签发/验签(开发阶段模拟监管端SSO)
 */
public class JwtUtils {

    private JwtUtils() {
    }

    public static String createToken(String secret, String userId, String userName, String role, long expireSeconds) {
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        Date now = new Date();
        return Jwts.builder()
                .claim(Constants.CLAIM_USER_ID, userId)
                .claim(Constants.CLAIM_USER_NAME, userName)
                .claim(Constants.CLAIM_ROLE, role)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expireSeconds * 1000))
                .signWith(key)
                .compact();
    }

    /**
     * @return claims, 验签失败或过期返回 null
     */
    public static Claims parse(String secret, String token) {
        try {
            SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
            return Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (Exception e) {
            return null;
        }
    }
}
