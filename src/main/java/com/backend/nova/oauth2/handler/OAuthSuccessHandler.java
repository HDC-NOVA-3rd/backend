package com.backend.nova.oauth2.handler;

import com.backend.nova.auth.jwt.JwtProvider;
import com.backend.nova.member.dto.MemberLocationResponse;
import com.backend.nova.member.dto.RedisMember;
import com.backend.nova.member.dto.TokenResponse;
import com.backend.nova.member.entity.Member;
import com.backend.nova.member.repository.MemberRepository;
import com.backend.nova.member.service.MemberService;
import com.backend.nova.member.service.RedisTokenService;
import com.backend.nova.oauth2.dto.CustomOAuth2User;
import com.backend.nova.oauth2.dto.OAuth2Response;
import com.backend.nova.oauth2.repository.AuthCodeInMemoryRepository;
import com.backend.nova.oauth2.repository.OAuthRedirectCookieRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;
import org.springframework.web.util.UriComponentsBuilder;
import com.backend.nova.auth.member.MemberDetails;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;

// [시점] UserService까지 문제없이 실행되고, 로그인이 '완전 성공' 했을 때 실행된다.
// 여기서 서버의 DB를 확인하고, 앱(App)에게 JWT 토큰을 돌려보내주는 로직 구성.
@Component
@Slf4j
@RequiredArgsConstructor
public class OAuthSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {
    private final JwtProvider jwtProvider;
    private final MemberRepository memberRepository;
    private final OAuthRedirectCookieRepository oAuthRedirectCookieRepository;
    private final AuthCodeInMemoryRepository authCodeRepository; // In memory 환경 token 저장소
    private final RedisTokenService redisTokenService;
    private final MemberService memberService;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        StopWatch stopWatch = new StopWatch("OAuth2 Success Handler");
        CustomOAuth2User customUser = (CustomOAuth2User) authentication.getPrincipal();
        OAuth2Response oAuthInfo = customUser.getOAuth2Response();
        log.info(String.valueOf(oAuthInfo));

        // ... (이메일, 프로필 정보 추출) ...
        String email = oAuthInfo.getEmail();
        String provider = oAuthInfo.getProvider();
        String providerId = oAuthInfo.getProviderId();
        String profileImg = oAuthInfo.getProfileImage();
        String phoneNumber = oAuthInfo.getPhoneNumber();
        String birthDate = oAuthInfo.getBirthDate();

        // 쿠키에서 redirect_uri 가져오기
        String targetUri = getRedirectUri(request);
        String targetUrl;
        // 랜덤 인증 코드 생성 -> 캐시에 로그인 / 회원가입 용 토큰 저장 목적
        String authCode = UUID.randomUUID().toString();

        // 1. 단일 트랜잭션으로 DB 조회 + 소셜정보 업데이트 + 연관 아파트 ID 매핑을 한 번에 처리
        stopWatch.start("1. DB Transaction (Find & Update & Map)");
        Optional<MemberDetails> optionalMemberDetails = memberService.processOAuthMemberLogin(email, provider, profileImg);
        stopWatch.stop();

        // [CASE 1] 기존 가입된 회원 -> 계정 연동 및 로그인 처리
        if (optionalMemberDetails.isPresent()) {
            MemberDetails memberDetails = optionalMemberDetails.get();

            stopWatch.start("2. JWT Creation");
            Authentication newAuth = new UsernamePasswordAuthenticationToken(memberDetails, null, memberDetails.getAuthorities());
            TokenResponse tokenResponse = jwtProvider.createTokenDto(newAuth, memberDetails.getMemberId(), memberDetails.getName());
            stopWatch.stop();

            stopWatch.start("3. Redis I/O");

            RedisMember dto = new RedisMember(
                    memberDetails.getMemberId(),
                    memberDetails.getUsername(),
                    memberDetails.getName(),
                    memberDetails.getApartmentId(),
                    memberDetails.getHoId(),
                    "MEMBER"
            );

            redisTokenService.saveAccessToken(tokenResponse.accessToken(), dto, jwtProvider.getAccessTokenExpires());
            redisTokenService.saveRefreshToken(memberDetails.getUsername(), tokenResponse.refreshToken(), jwtProvider.getRefreshTokenExpires());
            stopWatch.stop();

            // 메모리에 저장 (Code -> TokenResponse)
            authCodeRepository.save(authCode, tokenResponse);

            // 앱으로 돌아갈 URL 생성 (쿼리 파라미터에 status, code를 붙여서 전달)
            targetUrl = UriComponentsBuilder.fromUriString(targetUri)
                    .queryParam("status", "LOGIN") // 상태 구분값
                    .queryParam("code", authCode)
                    .build().encode(StandardCharsets.UTF_8).toUriString();

            log.info("로그인 성공. AuthCode 생성: {}", authCode);
        }
        // [CASE 2] 신규 회원 -> 회원가입 페이지로 이동
        else {
            // 회원가입 시 필요한 정보를 JWT(Register Token)에 저장 후, registerToken 으로 임시 저장
            String registerToken = jwtProvider.createRegisterToken(
                    email, oAuthInfo.getName(), provider, providerId, phoneNumber, birthDate
            );

            // 가입용 토큰도 메모리에 저장
            authCodeRepository.save(authCode, registerToken);

            targetUrl = UriComponentsBuilder.fromUriString(targetUri)
                    .queryParam("status", "REGISTER")
                    .queryParam("code", authCode)
                    .build().encode(StandardCharsets.UTF_8).toUriString();
            log.info("신규 회원. AuthCode 생성: {}", authCode);
        }
        // 3. 인증 관련 쿠키 삭제 (보안 및 용량 관리)
        oAuthRedirectCookieRepository.removeAuthorizationRequestCookies(request, response);

        log.info("[OAuth2 Success Handler 처리 시간]\n{}", stopWatch.prettyPrint());

        // 4. 리다이렉트 수행 (브라우저가 exp:// 스키마를 인식해서 앱을 켬)
        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }

    private String getRedirectUri(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookie.getName().equals(OAuthRedirectCookieRepository.REDIRECT_URI)) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }
}
