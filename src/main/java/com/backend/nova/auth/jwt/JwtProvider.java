package com.backend.nova.auth.jwt;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

@Component
public class JwtProvider {

    private final SecretKey secretKey;

    private static final long ACCESS_TOKEN_EXPIRE_MS = 1000L * 60 * 30;        // 30분
    private static final long REFRESH_TOKEN_EXPIRE_MS = 1000L * 60 * 60 * 24 * 14; // 14일

    public JwtProvider(@Value("${jwt.secret}") String secretKey) {
        this.secretKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secretKey));
    }

    /* ===================== 토큰 생성 ===================== */

    /**
     * 로그인 성공 시 Access + Refresh 발급
     * subject = loginId
     * role = Authority에서 추출
     */
    public JwtToken generateToken(Authentication authentication) {
        String subject = authentication.getName(); // loginId
        String role = authentication.getAuthorities()
                .iterator()
                .next()
                .getAuthority();

        return new JwtToken(
                createAccessToken(subject, role),
                createRefreshToken(subject)
        );
    }

    /**
     * Refresh 시 Access Token만 재발급
     */
    public String createAccessToken(Authentication authentication) {
        String subject = authentication.getName();
        String role = authentication.getAuthorities()
                .iterator()
                .next()
                .getAuthority();

        return createAccessToken(subject, role);
    }

    private String createAccessToken(String subject, String role) {
        return Jwts.builder()
                .subject(subject)
                .claim("role", role)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + ACCESS_TOKEN_EXPIRE_MS))
                .signWith(secretKey)
                .compact();
    }

    private String createRefreshToken(String subject) {
        return Jwts.builder()
                .subject(subject)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + REFRESH_TOKEN_EXPIRE_MS))
                .signWith(secretKey)
                .compact();
    }

    /* ===================== 검증 ===================== */

    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    /* ===================== 정보 추출 ===================== */

    public String getSubject(String token) {
        return parseClaims(token).getSubject();
    }

    public String getRole(String token) {
        return parseClaims(token).get("role", String.class);
    }

    // accessToken Payload(Claims)를 반환하는 메서드
    private Claims parseClaims(String accessToken) {
        try {
            return Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(accessToken).getPayload();
        } catch (ExpiredJwtException e) {
            return e.getClaims();
        }
    }

}
