package com.example.user.service.implementation;

import com.example.user.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.Date;
import java.util.function.Function;

@Service
public class JwtService {
    private final SecretKey key ;
    private final int accessTokenExpiration;
    private final int refreshTokenExpiration;

    public JwtService(@Value("${app.jwt.secret-key}") String secretKey,@Value("${app.jwt.access-token-expiration}") int accessTokenExpiration,
                      @Value("${app.jwt.refresh-token-expiration}") int refreshTokenExpiration) {
        this.accessTokenExpiration = accessTokenExpiration;
        this.refreshTokenExpiration = refreshTokenExpiration;
        byte[] keyBytes = Base64.getDecoder().decode(secretKey);
        this.key = Keys.hmacShaKeyFor(keyBytes);
    }
    public String generateAccessToken(User user) {
        return buildToken(user, 1000L * 60 * accessTokenExpiration);
    }

    public String generateRefreshToken(User user) {
        return buildToken(user, 1000L * 60 * 60 * 24 * refreshTokenExpiration); // 7 days
    }

    private String buildToken(User user, long expirationTime) {
        return Jwts.builder()
                .subject(user.getUserName())
                .claim("role", user.getRole())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationTime))
                .signWith(key)
                .compact();
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean isTokenValid(String token, String username) {
        final String extractedUsername = extractUsername(token);
        return (extractedUsername.equals(username) && !isTokenExpired(token));
    }

    private boolean isTokenExpired(String token) {
        return extractClaim(token, Claims::getExpiration).before(new Date());
    }
}