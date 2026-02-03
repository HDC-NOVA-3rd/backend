package com.backend.nova.auth.jwt;

import com.backend.nova.admin.entity.Admin;
import com.backend.nova.admin.repository.AdminRepository;
import com.backend.nova.auth.admin.AdminDetails;
import com.backend.nova.auth.member.MemberDetails;
import com.backend.nova.member.dto.TokenResponse;
import com.backend.nova.member.entity.Member;
import com.backend.nova.member.repository.MemberRepository;
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
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.stream.Collectors;

@Slf4j
@Component
public class JwtProvider {

    private final SecretKey secretKey; // 토큰 서명(암호화/복호화)에 사용할 비밀키 객체
    private final Long accessTokenExpires;
    private final Long refreshTokenExpires;

    private final AdminRepository adminRepository;
    private final MemberRepository memberRepository;

    public JwtProvider(
            @Value("${jwt.secret}") String secretStr,
            AdminRepository adminRepository,
            MemberRepository memberRepository
    ) {
        byte[] keyBytes = Decoders.BASE64.decode(secretStr); // secretStr을 BASE64로 Decode
        this.secretKey = Keys.hmacShaKeyFor(keyBytes);
        this.accessTokenExpires = 3600 * 1000L;    // 1시간
        this.refreshTokenExpires = 604800 * 1000L; // 7일
        this.adminRepository = adminRepository;
        this.memberRepository = memberRepository;
    }

    // 로그인 성공 시 new 토큰 생성 (Access + Refresh)
    public TokenResponse generateToken(Authentication authentication) {
        // 1. 권한 가져오기: 로그인한 사용자의 권한 리스트를 ","로 구분된 문자열로 만듦
        String authorities = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(","));

        long now = new Date().getTime();
        Date accessTokenExpiresIn = new Date(now + accessTokenExpires);
        Date refreshTokenExpiresIn = new Date(now + refreshTokenExpires);

        String accessToken = Jwts.builder()
                .subject(authentication.getName()) // adminId or memberId
                .claim("auth", authorities)         // 권한 정보
                .expiration(accessTokenExpiresIn)
                .signWith(secretKey)                // 비밀키 서명
                .compact();

        String refreshToken = Jwts.builder()
                .expiration(refreshTokenExpiresIn)
                .signWith(secretKey)
                .compact();

        return TokenResponse.builder()
                .grantType("Bearer")
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }

    // 검증된 토큰에서 인증 정보(Authentication) 추출 -> validateToken() 이후 실행
    public Authentication getAuthentication(String accessToken) {
        Claims claims = parseClaims(accessToken);

        if (claims.get("auth") == null) {
            throw new RuntimeException("권한 정보가 없는 토큰입니다.");
        }

        // Claims 에서 권한 정보 가져오기
        Collection<? extends GrantedAuthority> authorities =
                Arrays.stream(claims.get("auth").toString().split(","))
                        .map(SimpleGrantedAuthority::new)
                        .toList();
        // "ROLE_MEMBER", "ROLE_ADMIN", "ROLE_SUPER_ADMIN" 등

        String subject = claims.getSubject(); // adminId or memberId

        // ================= ADMIN =================
        if (authorities.stream().anyMatch(a -> a.getAuthority().startsWith("ROLE_ADMIN"))) {
            Long adminId = Long.parseLong(subject);
            Admin admin = adminRepository.findById(adminId)
                    .orElseThrow(() -> new RuntimeException("Admin not found"));

            AdminDetails principal = new AdminDetails(admin);

            return new UsernamePasswordAuthenticationToken(
                    principal, "", authorities
            );
        }

        // ================= MEMBER =================
        Long memberId = Long.parseLong(subject);
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new RuntimeException("Member not found"));

        MemberDetails principal = new MemberDetails(member);

        return new UsernamePasswordAuthenticationToken(
                principal, "", authorities
        );
    }

    // 토큰 유효성 검사 (요청 시 Filter에서 가장 먼저 실행)
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (io.jsonwebtoken.security.SecurityException | MalformedJwtException e) {
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

    // accessToken Payload(Claims)를 반환하는 메서드
    private Claims parseClaims(String accessToken) {
        try {
            return Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(accessToken)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            // 만료된 토큰이어도 Claims 는 반환
            return e.getClaims();
        }
    }
}
