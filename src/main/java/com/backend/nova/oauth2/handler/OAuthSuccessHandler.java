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
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

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

        String email = oAuthInfo.getEmail();
        String provider = oAuthInfo.getProvider(); // naver, google
        String providerId = oAuthInfo.getProviderId();
        String profileImg = oAuthInfo.getProfileImage();
        String phoneNumber = oAuthInfo.getPhoneNumber();
        String birthDate = oAuthInfo.getBirthDate();

        // 1. DB에서 회원 조회 (이메일 혹은 소셜ID로 조회)
        Optional<Member> optionalMember = memberRepository.findByEmail(email);

        // 2. 쿠키에서 redirect_uri 가져오기
        String targetUri = getRedirectUri(request);

        // 만약 쿠키에도 없고 기본값도 없으면 에러가 날 수 있으니 기본값 설정 (개발용)
        if (!StringUtils.hasText(targetUri)) {
            targetUri = "exp://192.168.14.116:8081/--/oauth/callback";
        }

        String targetUrl;

        if (optionalMember.isPresent()) {
            Member existMember = optionalMember.get();
            // [CASE 1] 이미 가입된 회원 -> 계정 연동 및 로그인 처리
            // 1-1. 정보 업데이트 로직 (더티 체킹 활용)
            // 기존에 NORAML 상태였다면, 로그인 타입과 프로필 사진을 최신화해준다.

            // 이후 "자연스럽게 로그인" 되도록 처리한다.
            existMember.updateOAuthInfo(provider, providerId,profileImg);
            memberRepository.save(existMember);

            // access 토큰 발급
            String token = jwtProvider.generateToken(authentication);



            targetUrl = UriComponentsBuilder.fromUriString(targetUri)
                    .queryParam("status", "LOGIN") // 상태 구분값
                    .queryParam("token", accessToken)
                    .queryParam("refreshToken",refreshToken)
                    .build()
                    .encode(StandardCharsets.UTF_8)
                    .toUriString();

            log.info("기존 회원(이메일 일치) 소셜 로그인 연동 및 성공: {}", email);

        } else {
            // [CASE 2] 신규 회원 -> 회원가입 페이지로 이동
            // 회원가입 시 필요한 정보를 JWT(Register Token)에 담아서 보냄 (보안상 URL에 평문 노출 지양)
            // createRegisterToken 메서드는 JwtProvider에 새로 만드세요 (유효시간 10분 정도로 짧게)
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
                    .queryParam("token", registerToken) // 가입용 임시 토큰
                    .build()
                    .encode(StandardCharsets.UTF_8)
                    .toUriString();

            log.info("신규 회원 가입 요청: {}", email);
        }

        // 3. 인증 관련 쿠키 삭제 (중요: 사용한 쿠키는 지워줘야 함)
        oAuthRedirectCookieRepository.removeAuthorizationRequestCookies(request, response);

        // 4. 리다이렉트 수행
        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }

    private String getRedirectUri(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookie.getName().equals(OAuthRedirectCookieRepository.REDIRECT_URI_PARAM_COOKIE_NAME)) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }
}
