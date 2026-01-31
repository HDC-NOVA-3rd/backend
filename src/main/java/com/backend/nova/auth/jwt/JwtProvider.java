package com.backend.nova.auth.jwt;

import com.backend.nova.member.dto.TokenResponse;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
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

    public JwtProvider(@Value("${jwt.secret}") String secretStr) {
        byte[] keyBytes = Decoders.BASE64.decode(secretStr); //secretStr을 BASE64로 Decode
        this.secretKey = Keys.hmacShaKeyFor(keyBytes);
        accessTokenExpires = 300 * 1000L; // 5분
        refreshTokenExpires = 604800 * 1000L; // 7일
    }

    public String createRegisterToken(String email, String name, String provider, String providerId, String phoneNumber, String birthDate) {
        Date now = new Date();
        Date validity = new Date(now.getTime() + 1000 * 60 * 10); // 10분만 유효

        return Jwts.builder()
                .subject("REGISTER_USER")      // 주제 설정
                .claim("email", email)         // 데이터 추가 (.put 대신 .claim 사용)
                .claim("name", name)
                .claim("provider", provider)
                .claim("providerId", providerId)
                .claim("phone", phoneNumber)
                .claim("birthDate", birthDate)
                .expiration(validity)          // 만료 시간
                .signWith(secretKey) // 서명
                .compact();
    }

    public JwtToken generateToken(Authentication authentication) {
        String accessToken = createAccessToken(authentication);
        String refreshToken = createRefreshToken(authentication);

        return JwtToken.builder()
                .grantType("Bearer")
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }

    // [신규] Access Token만 생성 (Refresh 요청 시 사용)
    public String createAccessToken(Authentication authentication) {
        String authorities = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(","));

        long now = (new Date()).getTime();
        Date accessTokenExpiresIn = new Date(now + accessTokenExpires);

        return Jwts.builder()
                .subject(authentication.getName())
                .claim("auth", authorities)
                .expiration(accessTokenExpiresIn)
                .signWith(secretKey)
                .compact();
    }

    // [신규] Refresh Token만 생성 (내부 호출용)
    public String createRefreshToken(Authentication authentication) {
        long now = (new Date()).getTime();
        Date refreshTokenExpiresIn = new Date(now + refreshTokenExpires);

        return Jwts.builder()
                .subject(authentication.getName())
                .expiration(refreshTokenExpiresIn)
                .signWith(secretKey)
                .compact();
    }

    // 검증된 토큰에서 인증 정보(Authentication) 추출 -> validateToken() 이후 실행
    public Authentication getAuthentication(String accessToken) {
        Claims claims = parseClaims(accessToken);

        if (claims.get("auth") == null) {
            throw new RuntimeException("권한 정보가 없는 토큰입니다.");
        }
        // 1. Claims 에서 권한 정보 가져오기
        Collection<? extends GrantedAuthority> authorities =
                Arrays.stream(claims.get("auth").toString().split(","))
                        .map(SimpleGrantedAuthority::new)
                        .collect(Collectors.toList());
        // "ROLE_USER,ROLE_ADMIN" 문자열 split -> GrantedAuthority 객체 리스트 변환

        // User: UserDetails 구현체
        // 사전에 검증된 토큰이므로 비밀번호는 빈 문자열("")로 둔다.
        UserDetails principal = new User(claims.getSubject(), "", authorities);
        return new UsernamePasswordAuthenticationToken(principal, "", authorities);
    }

    // 토큰 유효성 검사 (요청 시 Filter에서 가장 먼저 실행)
    public boolean validateToken(String token) {
        try {
            // secretKey 기반으로 입력된 token 파싱
            Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token);
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
    // 토큰에서 Subject(사용자 ID) 추출
    public String getSubject(String token) {
        return parseClaims(token).getSubject();
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
