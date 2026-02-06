package com.backend.nova.oauth2.handler;

import com.backend.nova.auth.jwt.JwtProvider;
import com.backend.nova.member.entity.Member;
import com.backend.nova.member.repository.MemberRepository;
import com.backend.nova.oauth2.dto.CustomOAuth2User;
import com.backend.nova.oauth2.dto.OAuth2Response;
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
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;
import com.backend.nova.auth.member.MemberDetails;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

// [시점] UserService까지 문제없이 실행되고, 로그인이 '완전 성공' 했을 때 실행된다.
// 여기서 서버의 DB를 확인하고, 앱(App)에게 JWT 토큰을 돌려보내주는 로직 구성.
@Component
@Slf4j
@RequiredArgsConstructor
public class OAuthSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {
    private final JwtProvider jwtProvider;
    private final MemberRepository memberRepository;
    private final OAuthRedirectCookieRepository oAuthRedirectCookieRepository; // 쿠키 삭제용

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
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

        // 1. DB에서 회원 조회 (이메일 기반 조회)
        Optional<Member> optionalMember = memberRepository.findByEmail(email);

        // 2. 쿠키에서 redirect_uri 가져오기
        String targetUri = getRedirectUri(request);

        String targetUrl;

        // [CASE 1] 기존 가입된 회원 -> 계정 연동 및 로그인 처리
        if (optionalMember.isPresent()) {
            Member existMember = optionalMember.get();

            // 1-1. 소셜 정보 업데이트
            // 기존에 NORMAL 상태면, 로그인 타입과 프로필 사진을 최신화한다.
            existMember.updateOAuthInfo(provider, providerId,profileImg);
            memberRepository.save(existMember);

            // OAuth 인증 객체 대신, DB의 Member 정보로 새로운 Authentication 생성
            // 이유: 이렇게 해야 토큰의 Subject에 'loginId'가 들어갑니다.
            MemberDetails memberDetails = new MemberDetails(existMember);
            Authentication newAuth = new UsernamePasswordAuthenticationToken(memberDetails,null, memberDetails.getAuthorities());

            // access + refresh 토큰 발급
            String accessToken = jwtProvider.createAccessToken(newAuth);
            String refreshToken = jwtProvider.createRefreshToken(newAuth);


            // 앱으로 돌아갈 URL 생성 (쿼리 파라미터에 토큰을 붙여서 전달)
            // ex) exp://...?status=LOGIN&token=eyJhbG...
            targetUrl = UriComponentsBuilder.fromUriString(targetUri)
                    .queryParam("status", "LOGIN") // 상태 구분값
                    .queryParam("token", accessToken)
                    .queryParam("refreshToken",refreshToken)
                    .build()
                    .encode(StandardCharsets.UTF_8)
                    .toUriString();
            log.info("기존 회원(이메일 일치) 소셜 로그인 연동 및 성공: {}", email);

        }
        // [CASE 2] 신규 회원 -> 회원가입 페이지로 이동
        else {
            // 회원가입 시 필요한 정보를 JWT(Register Token)에 담아서 보냄 (보안상 URL에 평문 노출 지양)
            String registerToken = jwtProvider.createRegisterToken(
                    email,
                    oAuthInfo.getName(),
                    provider,
                    providerId,
                    phoneNumber,
                    birthDate
            );

            targetUrl = UriComponentsBuilder.fromUriString(targetUri)
                    .queryParam("status", "REGISTER") // 상태 구분값
                    .queryParam("token", registerToken) // 이 토큰을 가지고 앱이 다시 회원가입 API(/signup)를 호출함
                    .build().encode(StandardCharsets.UTF_8).toUriString();
            log.info("신규 회원 가입 요청: {}", email);
        }

        // 3. 인증 관련 쿠키 삭제 (보안 및 용량 관리)
        oAuthRedirectCookieRepository.removeAuthorizationRequestCookies(request, response);

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
