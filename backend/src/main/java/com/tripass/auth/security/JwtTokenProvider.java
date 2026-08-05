package com.tripass.auth.security;

import com.tripass.auth.model.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

//JWT 생성과 검증을 담당한다.
@Component
public class JwtTokenProvider {

    private static final String TOKEN_TYPE_CLAIM="tokenType";
    private static final String ACCESS_TOKEN_TYPE = "ACCESS";
    private static final String REFRESH_TOKEN_TYPE = "REFRESH";

    private final SecretKey signingKey;
    private final long accessExpirationMs;
    private final long refreshExpirationMs;

    public JwtTokenProvider(@Value("${jwt.secret}") String secret,
                            @Value("${jwt.access.expiration.ms}") long accessExpirationMs,
                            @Value("${jwt.refresh.expiration.ms}") long refreshExpirationMs
                            ){
        validateConfiguration(secret, accessExpirationMs, refreshExpirationMs);

        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessExpirationMs = accessExpirationMs;
        this.refreshExpirationMs = refreshExpirationMs;
    }
    //로그인 유저의 Access Token 생성
    public String createAccessToken(User user){
        Date issuedAt = new Date();
        Date expiresAt = new Date(
                issuedAt.getTime() + accessExpirationMs
        );
        return Jwts.builder()
                .setSubject(String.valueOf(user.getId()))
                .claim("loginId", user.getLoginId())
                .claim("loginProvider", user.getLoginProvider())
                .claim(TOKEN_TYPE_CLAIM, ACCESS_TOKEN_TYPE)
                .setIssuedAt(issuedAt)
                .setExpiration(expiresAt)
                .signWith(signingKey, SignatureAlgorithm.HS256)
                .compact();
    }
    // Refresh Token 생성
    public String createRefreshToken(User user){
        Date issuedAt = new Date();
        Date expiresAt = new Date(
                issuedAt.getTime() + refreshExpirationMs
        );
        return Jwts.builder()
                .setSubject(String.valueOf(user.getId()))
                .setId(UUID.randomUUID().toString())
                .claim(TOKEN_TYPE_CLAIM, REFRESH_TOKEN_TYPE)
                .setIssuedAt(issuedAt)
                .setExpiration(expiresAt)
                .signWith(signingKey, SignatureAlgorithm.HS256)
                .compact();
    }
    // Access Token 서명, 만료시간 검증
    public boolean validateAccessToken(String token){
        try{
            Claims claims = parseClaims(token);
            return ACCESS_TOKEN_TYPE.equals(
                    claims.get(TOKEN_TYPE_CLAIM, String.class)
            );
        }catch (JwtException | IllegalArgumentException exception){
            return false;
        }
    }
    // Refresh Token 서명, 만료시간 검증
    public boolean validateRefreshToken(String token){
        try{
            Claims claims = parseClaims(token);
            return REFRESH_TOKEN_TYPE.equals(
                    claims.get(TOKEN_TYPE_CLAIM, String.class)
            );
        }catch (JwtException | IllegalArgumentException exception){
            return false;
        }
    }

    //JWT에서 회원 PK 조회
    public Long getUserId(String token){
        String subject = parseClaims(token).getSubject();
        return Long.valueOf(subject);
    }
    // JWT 고유 식별값 조회
    public String getTokenId(String token) {
        return parseClaims(token).getId();
    }
    // JWT 만료일시 조회
    public Date getExpirationDate(String token) {
        return parseClaims(token).getExpiration();
    }
    //JWT에서 로그인 아이디 조회
    public String getLoginId(String token){
        return parseClaims(token).get("loginId",String.class);
    }
    //JWT에서 로그인 방식 조회
    public String getLoginProvider(String token){
        return parseClaims(token).get("loginProvider", String.class);
    }
    //Acces Token 만료시간 반환
    public long getAccessExpirationSeconds(){
        return accessExpirationMs / 1000;
    }
    // Refresh Token 만료시간을 초 단위로 반환
    public long getRefreshExpirationSeconds() {
        return refreshExpirationMs / 1000;
    }
    private Claims parseClaims(String token){
        return Jwts.parserBuilder()
                .setSigningKey(signingKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private void validateConfiguration(String secret, long accessExpirationMs, long refreshExpirationMs){
        if(secret == null || secret.getBytes(StandardCharsets.UTF_8).length<32){
            throw new IllegalStateException("JWT 비밀키는 32바이트 이상으로 설정하세요");
        }
        if(accessExpirationMs<=0 || refreshExpirationMs<=0){
            throw new IllegalStateException("JWT 만료시간은 0보다 크게 설정하세요");
        }
        if (refreshExpirationMs <= accessExpirationMs) {
            throw new IllegalStateException("Refresh Token 만료시간은 Access Token보다 길어야 합니다.");
        }
    }
}
