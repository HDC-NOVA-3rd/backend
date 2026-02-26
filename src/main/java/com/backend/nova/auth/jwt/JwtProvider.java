package com.backend.nova.auth.jwt;

import com.backend.nova.admin.entity.Admin;
import com.backend.nova.auth.admin.AdminDetails;
import com.backend.nova.auth.admin.AdminDetailsService;
import com.backend.nova.auth.member.MemberDetails;
import com.backend.nova.auth.member.MemberDetailsService;
import com.backend.nova.member.dto.RedisMember;
import com.backend.nova.member.dto.TokenResponse;
import com.backend.nova.member.service.RedisTokenService;
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

    private final SecretKey secretKey; // 토큰 서명(암호화/복호화)에 사용할 비밀키 객체
    private final Long accessTokenExpires;
    private final Long refreshTokenExpires;
    private final AdminDetailsService adminDetailsService;
    private final RedisTokenService redisTokenService;

    public JwtProvider(
            @Value("${jwt.secret}") String secretStr,
            AdminDetailsService adminDetailsService,
            RedisTokenService redisTokenService
    ) {
        byte[] keyBytes = Decoders.BASE64.decode(secretStr); //secretStr을 BASE64로 Decode
        this.secretKey = Keys.hmacShaKeyFor(keyBytes);
        accessTokenExpires = 300 * 1000L; // 5분
        refreshTokenExpires = 604800 * 1000L; // 7일
        this.adminDetailsService = adminDetailsService;
        this.redisTokenService = redisTokenService;
    }

    /**
     * 로그인 성공 시 반환할 TokenResponse DTO 생성
     * Access/Refresh 토큰을 발급하고, 프론트엔드에 필요한 사용자 정보와 함께 묶어서 반환
     */
    public TokenResponse createTokenDto(Authentication authentication, Long memberId, String name) {
        String accessToken = createAccessToken(authentication);
        String refreshToken = createRefreshToken(authentication);
        log.info("인증된 객체를 기반으로 Member access/refresh 토큰 발급 {} / {}",accessToken,refreshToken);
        return TokenResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .memberId(memberId)
                .name(name)
                .build();
    }

    // Register Token (OAuth User가 회원가입이 필요한 경우 발급하는 토큰)
    public String createRegisterToken(String email, String name, String provider, String providerId, String phoneNumber, String birthDate) {
        Date now = new Date();
        Date validity = new Date(now.getTime() + 1000 * 60 * 10); // 10분만 유효
        log.info("register 토큰 발급 완료");
        return Jwts.builder()
                .subject("REGISTER_USER")      // 주제 설정
                .claim("email", email)   // 데이터 추가 (.put 대신 .claim 사용)
                .claim("name", name)
                .claim("provider", provider)
                .claim("providerId", providerId)
                .claim("phone", phoneNumber)
                .claim("birthDate", birthDate)
                .expiration(validity)          // 만료 시간
                .signWith(secretKey) // 서명
                .compact();
    }

    /* ================== 토큰 생성 ================== */

    public JwtToken generateToken(Authentication authentication) {
        return JwtToken.builder()
                .accessToken(createAccessToken(authentication))
                .refreshToken(createRefreshToken(authentication.getName()))
                .build();
    }

    public JwtToken generateAdminToken(Admin admin) {
        AdminDetails adminDetails = new AdminDetails(admin);

        Authentication authentication =
                new UsernamePasswordAuthenticationToken(
                        adminDetails,
                        null,
                        adminDetails.getAuthorities()
                );

        return generateToken(authentication);
    }

    // [신규] Access Token만 생성 (Refresh 요청 시 사용)
    public String createAccessToken(Authentication authentication) {
        String authorities = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(","));

        // Principal에서 apartmentId 추출
        Long apartmentId = null;
        Object principal = authentication.getPrincipal();
        if (principal instanceof MemberDetails) {
            apartmentId = ((MemberDetails) principal).getApartmentId();
        } else if (principal instanceof AdminDetails) {
            apartmentId = ((AdminDetails) principal).getApartmentId();
        }

        long now = (new Date()).getTime();
        Date accessTokenExpiresIn = new Date(now + accessTokenExpires);

        return Jwts.builder()
                .subject(authentication.getName())
                .claim("auth", authorities)
                .claim("apartmentId",apartmentId)
                .expiration(accessTokenExpiresIn)
                .signWith(secretKey)
                .compact();
    }

    public String createRefreshToken(String subject) {
        return Jwts.builder()
                .subject(subject)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + refreshTokenExpires))
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
        String authString = claims.get("auth").toString();
        Collection<? extends GrantedAuthority> authorities =
                Arrays.stream(authString.split(","))
                        .map(SimpleGrantedAuthority::new)
                        .toList();
        String loginId = claims.getSubject();

        if(authString.contains("ADMIN")){
            AdminDetails admin =
                    (AdminDetails) adminDetailsService.loadUserByUsername(loginId);
            return new UsernamePasswordAuthenticationToken(admin, "", authorities);
        }
        else{
            RedisMember redisMember = redisTokenService.getRedisMemberByAccessToken(accessToken);
            if(redisMember == null){
                throw new JwtException("유효하지 않거나 로그아웃된 토큰입니다.");
            }

            MemberDetails memberDetails = new MemberDetails(
                    redisMember.memberId(), redisMember.loginId(), redisMember.name(),
                    redisMember.apartmentId(), redisMember.hoId(), authString
            );
            return new UsernamePasswordAuthenticationToken(memberDetails, "", authorities);
        }
    }

    // Access 토큰 유효성 검사 (요청 시 Filter에서 가장 먼저 실행)
    public boolean validateToken(String token) {
        try {
            // secretKey 기반으로 입력된 token 파싱
            Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token);
            return true;
        } catch (io.jsonwebtoken.security.SecurityException | MalformedJwtException e) {
            log.info("잘못된 JWT 서명입니다.");
            throw new JwtException("잘못된 JWT 서명입니다.");
        } catch (ExpiredJwtException e) {
            log.info("만료된 JWT 토큰입니다.");
            throw e;
        } catch (IllegalArgumentException e) {
            log.info("JWT 토큰이 잘못되었습니다.");
            throw e;
        }
        catch (UnsupportedJwtException e) {
            log.info("지원되지 않는 JWT 토큰입니다.");
        }
        return false;
    }
    // 토큰에서 Subject(사용자 ID) 추출
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
    public Long getApartmentId(String token) {
        return parseClaims(token).get("apartmentId", Long.class);
    }
    public long getAccessTokenExpires() {
        return accessTokenExpires; // 300 * 1000L (5분)
    }

    public long getRefreshTokenExpires() {
        return refreshTokenExpires; // 604800 * 1000L (7일)
    }
}
