package com.backend.nova.auth.jwt;

import com.backend.nova.auth.admin.AdminDetails;
import com.backend.nova.auth.admin.AdminDetailsService;
import com.backend.nova.auth.member.MemberDetails;
import com.backend.nova.auth.member.MemberDetailsService;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
public class JwtProvider {

    /* ================== 설정값 ================== */

    private final SecretKey secretKey;
    private final long accessTokenExpireMs;
    private final long refreshTokenExpireMs;
    private final long registerTokenExpireMs;

    private final AdminDetailsService adminDetailsService;
    private final MemberDetailsService memberDetailsService;

    public JwtProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-token-expire-ms}") long accessTokenExpireMs,
            @Value("${jwt.refresh-token-expire-ms}") long refreshTokenExpireMs,
            @Value("${jwt.register-token-expire-ms}") long registerTokenExpireMs,
            AdminDetailsService adminDetailsService,
            MemberDetailsService memberDetailsService
    ) {
        this.secretKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
        this.accessTokenExpireMs = accessTokenExpireMs;
        this.refreshTokenExpireMs = refreshTokenExpireMs;
        this.registerTokenExpireMs = registerTokenExpireMs;
        this.adminDetailsService = adminDetailsService;
        this.memberDetailsService = memberDetailsService;
    }

    /* ================== 토큰 생성 ================== */

    public JwtToken generateToken(Authentication authentication) {
        return JwtToken.builder()
                .accessToken(createAccessToken(authentication))
                .refreshToken(createRefreshToken(authentication.getName()))
                .build();
    }

    public String createAccessToken(Authentication authentication) {
        String authorities = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(","));

        return Jwts.builder()
                .subject(authentication.getName())
                .claim("auth", authorities)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + accessTokenExpireMs))
                .signWith(secretKey)
                .compact();
    }

    public String createRefreshToken(String subject) {
        return Jwts.builder()
                .subject(subject)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + refreshTokenExpireMs))
                .signWith(secretKey)
                .compact();
    }

    public String createRefreshToken(Authentication authentication) {
        return createRefreshToken(authentication.getName());
    }

    /* ================== OAuth 신규 회원 ================== */

    public String createRegisterToken(
            String email,
            String name,
            String provider,
            String providerId,
            String phoneNumber,
            String birthDate
    ) {
        return Jwts.builder()
                .subject("REGISTER")
                .claim("email", email)
                .claim("name", name)
                .claim("provider", provider)
                .claim("providerId", providerId)
                .claim("phoneNumber", phoneNumber)
                .claim("birthDate", birthDate)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + registerTokenExpireMs))
                .signWith(secretKey)
                .compact();
    }

    /* ================== Authentication 복원 ================== */

    public Authentication getAuthentication(String accessToken) {
        Claims claims = parseClaims(accessToken);

        String auth = claims.get("auth", String.class);
        if (auth == null) {
            throw new RuntimeException("권한 정보가 없는 토큰입니다.");
        }

        Collection<? extends GrantedAuthority> authorities =
                Arrays.stream(auth.split(","))
                        .map(SimpleGrantedAuthority::new)
                        .toList();

        String loginId = claims.getSubject();

        try {
            AdminDetails admin =
                    (AdminDetails) adminDetailsService.loadUserByUsername(loginId);
            return new UsernamePasswordAuthenticationToken(admin, "", authorities);
        } catch (Exception e) {
            MemberDetails member =
                    (MemberDetails) memberDetailsService.loadUserByUsername(loginId);
            return new UsernamePasswordAuthenticationToken(member, "", authorities);
        }
    }

    /* ================== 검증 ================== */

    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.info("JWT 검증 실패: {}", e.getMessage());
            return false;
        }
    }

    /* ================== 유틸 ================== */

    public String getSubject(String token) {
        return parseClaims(token).getSubject();
    }

    private Claims parseClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            return e.getClaims();
        }
    }
}
