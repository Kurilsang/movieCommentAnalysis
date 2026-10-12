package com.moviereview.user;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 工具类。
 */
@Component
public class JwtUtil {

    @Value("${jwt.secret:mySecretKeyForMovieSentimentSystem2024VeryLongAndSecure}")
    private String secret;

    @Value("${jwt.expiration:86400000}") // 24 小时
    private long expiration;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 生成 JWT token。
     */
    public String generateToken(Long userId, String username, String role) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expiration);
        return Jwts.builder()
                .setSubject(username)
                .claim("userId", userId)
                .claim("role", role)
                .setIssuedAt(now)
                .setExpiration(expiry)
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * 解析 JWT token。
     *
     * @return Claims
     * @throws JwtException token 无效或过期
     */
    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * 从 token 中提取用户名。
     */
    public String getUsername(String token) {
        return parseToken(token).getSubject();
    }

    /**
     * 从 token 中提取角色。
     */
    public String getRole(String token) {
        return parseToken(token).get("role", String.class);
    }

    /**
     * 从 token 中提取用户 ID。
     */
    public Long getUserId(String token) {
        return parseToken(token).get("userId", Long.class);
    }
}
