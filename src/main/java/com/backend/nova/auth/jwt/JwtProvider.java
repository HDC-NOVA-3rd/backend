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
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
public class JwtProvider {

    /* ================== JWT 기본 설정 ================== */

    private final SecretKey secretKey;                 // JWT 서명용 비밀키
    private final Long accessTokenExpires;              // Access Token 만료시간
    private final Long refreshTokenExpires;             // Refresh Token 만료시간

    private final AdminRepository adminRepository;
    private final MemberRepository memberRepository;

    public JwtProvider(
            @Value("${jwt.secret}") String secretStr,
            AdminRepository adminRepository,
            MemberRepository memberRepository
    ) {
        byte[] keyBytes = Decoders.BASE64.decode(secretStr);
        this.secretKey = Keys.hmacShaKeyFor(keyBytes);

        this.accessTokenExpires = 3600 * 1000L;      // 1시간
        this.refreshTokenExpires = 604800 * 1000L;   // 7일

        this.adminRepository = adminRepository;
        this.memberRepository = memberRepository;
    }

    /* ================== 회원가입용 임시 토큰 ================== */

    // OAuth 회원가입 완료 전 임시 토큰 (10분 유효)
    public String createRegisterToken(
            String email,
            String name,
            String provider,
            String providerId,
            String phoneNumber,
            String birthDate
    ) {
        Date now = new Date();
        Date validity = new Date(now.getTime() + 1000 * 60 * 10);

        return Jwts.builder()
                .subject("REGISTER_USER")
                .claim("email", email)
                .claim("name", name)
                .claim("provider", provider)
                .claim("providerId", providerId)
                .claim("phone", phoneNumber)
                .claim("birthDate", birthDate)
                .expiration(validity)
                .signWith(secretKey)
                .compact();
    }

    /* ================== 로그인 토큰 생성 ================== */

    // 로그인 성공 시 Access + Refresh Token 발급
    public TokenResponse generateToken(Authentication authentication) {

        String accessToken = createAccessToken(authentication);
        String refreshToken = createRefreshToken(authentication);

        Long memberId = null;
        String name = null;

        /* ================= ADMIN ================= */
        if (authentication.getPrincipal() instanceof AdminDetails adminDetails) {
            memberId = adminDetails.getAdminId();
            name = adminDetails.getLoginId(); // 또는 Admin 엔티티에 name 있으면 그걸로
        }

        /* ================= MEMBER ================= */
        if (authentication.getPrincipal() instanceof MemberDetails memberDetails) {
            memberId = memberDetails.getMemberId();
            name = memberDetails.getName();
        }

        return TokenResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .memberId(memberId)
                .name(name)
                .build();
    }



    // Access Token 생성 (Refresh 요청 시 재사용)
    public String createAccessToken(Authentication authentication) {
        String authorities = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(","));

        long now = System.currentTimeMillis();
        Date expiresIn = new Date(now + accessTokenExpires);

        return Jwts.builder()
                .subject(authentication.getName()) // adminId or memberId
                .claim("auth", authorities)
                .expiration(expiresIn)
                .signWith(secretKey)
                .compact();
    }

    // Refresh Token 생성
    public String createRefreshToken(Authentication authentication) {
        long now = System.currentTimeMillis();
        Date expiresIn = new Date(now + refreshTokenExpires);

        return Jwts.builder()
                .subject(authentication.getName())
                .expiration(expiresIn)
                .signWith(secretKey)
                .compact();
    }

    /* ================== 인증 객체 생성 ================== */

    // Access Token → Authentication 변환
    public Authentication getAuthentication(String accessToken) {
        Claims claims = parseClaims(accessToken);

        if (claims.get("auth") == null) {
            throw new RuntimeException("권한 정보가 없는 토큰입니다.");
        }

        // 권한 문자열 → GrantedAuthority 리스트
        Collection<? extends GrantedAuthority> authorities =
                Arrays.stream(claims.get("auth").toString().split(","))
                        .map(SimpleGrantedAuthority::new)
                        .toList();

        String subject = claims.getSubject(); // adminId or memberId

        /* ================= ADMIN ================= */
        if (authorities.stream().anyMatch(a -> a.getAuthority().startsWith("ROLE_ADMIN"))) {
            Long adminId = Long.parseLong(subject);
            Admin admin = adminRepository.findById(adminId)
                    .orElseThrow(() -> new RuntimeException("Admin not found"));

            AdminDetails principal = new AdminDetails(admin);
            return new UsernamePasswordAuthenticationToken(principal, "", authorities);
        }

        /* ================= MEMBER ================= */
        Long memberId = Long.parseLong(subject);
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new RuntimeException("Member not found"));

        MemberDetails principal = new MemberDetails(member);
        return new UsernamePasswordAuthenticationToken(principal, "", authorities);
    }

    /* ================== 토큰 검증 ================== */

    // 토큰 유효성 검사
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

    // 토큰에서 Subject 추출
    public String getSubject(String token) {
        return parseClaims(token).getSubject();
    }

    // Claims 파싱 (만료된 토큰도 Claims 반환)
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
