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

    /* ================== JWT 기본 설정 ================== */

    private final SecretKey secretKey;
    private final long accessTokenExpires;

    private final AdminDetailsService adminDetailsService;
    private final MemberDetailsService memberDetailsService;

    public JwtProvider(
            @Value("${jwt.secret}") String secretStr,
            AdminDetailsService adminDetailsService,
            MemberDetailsService memberDetailsService
    ) {
        byte[] keyBytes = Decoders.BASE64.decode(secretStr);
        this.secretKey = Keys.hmacShaKeyFor(keyBytes);
        this.accessTokenExpires = 60 * 60 * 1000L; // 1시간
        this.adminDetailsService = adminDetailsService;
        this.memberDetailsService = memberDetailsService;
    }

    /* ================== 회원가입용 임시 토큰 ================== */

    public String createRegisterToken(
            String email,
            String name,
            String provider,
            String providerId,
            String phoneNumber,
            String birthDate
    ) {
        Date expiresIn = new Date(System.currentTimeMillis() + 10 * 60 * 1000);

        return Jwts.builder()
                .subject("REGISTER_USER")
                .claim("email", email)
                .claim("name", name)
                .claim("provider", provider)
                .claim("providerId", providerId)
                .claim("phone", phoneNumber)
                .claim("birthDate", birthDate)
                .expiration(expiresIn)
                .signWith(secretKey)
                .compact();
    }

    /* ================== Access Token 생성 ================== */

    public String createAccessToken(Authentication authentication) {
        String authorities = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(","));

        Date expiresIn = new Date(System.currentTimeMillis() + accessTokenExpires);

        return Jwts.builder()
                .subject(authentication.getName()) // loginId
                .claim("auth", authorities)
                .expiration(expiresIn)
                .signWith(secretKey)
                .compact();
    }

    /* ================== Access Token → Authentication ================== */

    public Authentication getAuthentication(String accessToken) {
        Claims claims = parseClaims(accessToken);

        if (claims.get("auth") == null) {
            throw new RuntimeException("권한 정보가 없는 토큰입니다.");
        }

        Collection<? extends GrantedAuthority> authorities =
                Arrays.stream(claims.get("auth").toString().split(","))
                        .map(SimpleGrantedAuthority::new)
                        .toList();

        String loginId = claims.getSubject();

        try {
            //  Admin 우선
            AdminDetails admin =
                    (AdminDetails) adminDetailsService.loadUserByUsername(loginId);

            return new UsernamePasswordAuthenticationToken(admin, "", authorities);

        } catch (Exception e) {
            //  아니면 Member
            MemberDetails member =
                    (MemberDetails) memberDetailsService.loadUserByUsername(loginId);

            return new UsernamePasswordAuthenticationToken(member, "", authorities);
        }
    }

    /* ================== 토큰 검증 ================== */

    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (SecurityException | MalformedJwtException e) {
            log.info("잘못된 JWT 서명입니다.");
        } catch (ExpiredJwtException e) {
            log.info("만료된 JWT 토큰입니다.");
        } catch (UnsupportedJwtException e) {
            log.info("지원되지 않는 JWT 토큰입니다.");
        } catch (IllegalArgumentException e) {
            log.info("JWT 토큰이 잘못되었습니다.");
        }
        return false;
    }

    /* ================== 공통 유틸 ================== */

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
