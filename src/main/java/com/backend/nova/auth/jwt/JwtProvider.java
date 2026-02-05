package com.backend.nova.auth.jwt;

import com.backend.nova.admin.dto.AdminChallengeToken;
import com.backend.nova.admin.entity.Admin;
import com.backend.nova.admin.entity.AdminChallengePurpose;
import com.backend.nova.admin.repository.AdminRepository;
import com.backend.nova.auth.admin.AdminDetails;
import com.backend.nova.auth.member.MemberDetails;
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
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
public class JwtProvider {

    /* ================== JWT 기본 설정 ================== */

    private final SecretKey secretKey;                 // JWT 서명용 비밀키
    private final long accessTokenExpires = 1000L * 60 * 60;      // Access Token 만료시간 1시간
    private final long refreshTokenExpires = 1000L * 60 * 60 * 24 * 7; // Refresh Token 만료시간 7일

    private final AdminRepository adminRepository;
    private final MemberRepository memberRepository;

    public JwtProvider(
            @Value("${jwt.secret}") String secretStr,
            AdminRepository adminRepository,
            MemberRepository memberRepository
    ) {
        byte[] keyBytes = Decoders.BASE64.decode(secretStr);
        this.secretKey = Keys.hmacShaKeyFor(keyBytes);

        this.adminRepository = adminRepository;
        this.memberRepository = memberRepository;
    }

    /* ================== Admin Access Token ================== */

    public String createAdminAccessToken(
            Long adminId,
            String authority,
            String deviceId
    ) {
        long now = System.currentTimeMillis();
        Date expiry = new Date(now + accessTokenExpires);

        return Jwts.builder()
                .subject(String.valueOf(adminId))
                .claim("auth", authority)
                .claim("deviceId", deviceId)
                .issuedAt(new Date(now))
                .expiration(expiry)
                .signWith(secretKey)
                .compact();
    }

    /* ================== OTP Challenge Token ================== */

    public String createAdminChallengeToken(
            String loginId,
            AdminChallengePurpose purpose,
            Duration duration
    ) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + duration.toMillis());

        return Jwts.builder()
                .subject(loginId)
                .claim("type", "CHALLENGE")
                .claim("purpose", purpose.name())
                .issuedAt(now)
                .expiration(expiry)
                .signWith(secretKey)
                .compact();
    }

    public AdminChallengeToken parseAdminChallengeToken(String token) {
        Claims claims = parseClaims(token);

        if (!"CHALLENGE".equals(claims.get("type"))) {
            throw new RuntimeException("Challenge Token이 아닙니다");
        }

        return new AdminChallengeToken(
                claims.getSubject(),
                AdminChallengePurpose.valueOf(claims.get("purpose").toString())
        );
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
        Date now = new Date();
        Date validity = new Date(now.getTime() + 1000 * 60 * 10); // 10분 유효

        return Jwts.builder()
                .subject("REGISTER_USER")
                .claim("email", email)
                .claim("name", name)
                .claim("provider", provider)
                .claim("providerId", providerId)
                .claim("phone", phoneNumber)
                .claim("birthDate", birthDate)
                .issuedAt(now)
                .expiration(validity)
                .signWith(secretKey)
                .compact();
    }

    /* ================== 로그인 토큰 생성 ================== */

    public JwtToken generateToken(Authentication authentication) {
        return JwtToken.builder()
                .accessToken(createAccessToken(authentication))
                .refreshToken(createRefreshToken(authentication))
                .grantType("Bearer")
                .build();
    }

    private String createAccessToken(Authentication authentication) {
        String authorities = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(","));

        long now = System.currentTimeMillis();
        Date expiry = new Date(now + accessTokenExpires);

        return Jwts.builder()
                .subject(authentication.getName())
                .claim("auth", authorities)
                .issuedAt(new Date(now))
                .expiration(expiry)
                .signWith(secretKey)
                .compact();
    }

    private String createRefreshToken(Authentication authentication) {
        long now = System.currentTimeMillis();
        Date expiry = new Date(now + refreshTokenExpires);

        return Jwts.builder()
                .subject(authentication.getName())
                .issuedAt(new Date(now))
                .expiration(expiry)
                .signWith(secretKey)
                .compact();
    }

    /* ================== 인증 객체 생성 ================== */

    public Authentication getAuthentication(String accessToken) {
        Claims claims = parseClaims(accessToken);

        String auth = (String) claims.get("auth");
        if (auth == null) throw new RuntimeException("권한 정보가 없는 토큰입니다.");

        Collection<? extends GrantedAuthority> authorities =
                Arrays.stream(auth.split(","))
                        .map(SimpleGrantedAuthority::new)
                        .toList();

        Long id = Long.parseLong(claims.getSubject());

        // Admin 권한 체크
        if (auth.contains("ROLE_ADMIN")) {
            Admin admin = adminRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Admin not found"));
            AdminDetails principal = new AdminDetails(admin);
            return new UsernamePasswordAuthenticationToken(principal, "", authorities);
        }

        // Member 권한 처리
        Member member = memberRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Member not found"));
        MemberDetails principal = new MemberDetails(member);

        return new UsernamePasswordAuthenticationToken(principal, "", authorities);
    }

    public Authentication getAuthenticationFromRefreshToken(String refreshToken) {
        Claims claims = parseClaims(refreshToken);
        Long id = Long.parseLong(claims.getSubject());

        if (adminRepository.existsById(id)) {
            Admin admin = adminRepository.findById(id).orElseThrow();
            AdminDetails details = new AdminDetails(admin);
            return new UsernamePasswordAuthenticationToken(details, "", details.getAuthorities());
        }

        Member member = memberRepository.findById(id).orElseThrow();
        MemberDetails details = new MemberDetails(member);
        return new UsernamePasswordAuthenticationToken(details, "", details.getAuthorities());
    }

    /* ================== 토큰 검증 ================== */

    public boolean validateToken(String token) {
        try {
            Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token);
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
