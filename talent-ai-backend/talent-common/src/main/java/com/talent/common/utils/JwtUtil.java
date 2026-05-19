package com.talent.common.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

import java.security.Key;
import java.util.Date;

public class JwtUtil {

    // 密钥，必须大于 256 bit（32个字符以上）。实际项目中建议写在 yml 配置文件里
    private static final String SECRET_KEY = "TalentAiBackendSecretKeyForJwtAuthenticationSuperSecure";
    // 过期时间：默认设置 24 小时
    private static final long EXPIRATION_TIME = 1000 * 60 * 60 * 24;

    private static final Key key = Keys.hmacShaKeyFor(SECRET_KEY.getBytes());

    /**
     * 生成 Token
     * @param userId 用户的 ID
     * @param role 用户的角色 (例如 HR, CANDIDATE)
     */
    public static String generateToken(String userId, String role) {
        return Jwts.builder()
                .setSubject(userId) // 存入用户ID
                .claim("role", role) // 存入自定义权限/角色信息
                .setIssuedAt(new Date()) // 签发时间
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME)) // 过期时间
                .signWith(key, SignatureAlgorithm.HS256) // 签名算法
                .compact();
    }

    /**
     * 解析 Token
     */
    public static Claims parseToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}