package com.backend.nova.oauth2.repository;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.SerializationUtils;
import org.springframework.util.StringUtils;

import java.util.Base64;
import java.util.Optional;

/**
 * [OAuth2 인증 요청 및 Redirect URI 보존을 위한 쿠키 기반 레포지토리]
 * 소셜 로그인 페이지로 리다이렉트하기 직전에 호출되는 코드
 */
@Component
public class OAuthRedirectCookieRepository implements AuthorizationRequestRepository<OAuth2AuthorizationRequest> {

    // OAuth2 인증 요청 정보를 담을 쿠키 이름
    public static final String OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME = "oauth2_auth_request";
    // 앱에서 파라미터로 보낸 redirect_uri를 담을 쿠키 이름
    public static final String REDIRECT_URI = "redirect_uri";
    // 쿠키 유효 시간 (3분: 인증 완료하기에 충분한 시간)
    private static final int cookieExpireSeconds = 180;

    /**
     * 쿠키에 저장된 OAuth2 인증 요청 정보를 읽어옵니다.
     * 소셜 로그인 완료 후 우리 서버로 돌아왔을 때 호출됩니다.
     */
    @Override
    public OAuth2AuthorizationRequest loadAuthorizationRequest(HttpServletRequest request) {
        return getCookie(request, OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME)
                .map(cookie -> deserialize(cookie, OAuth2AuthorizationRequest.class))
                .orElse(null);
    }

    /**
     * [시점] 앱이 OAuth 로그인 버튼을 눌러 서버로 들어왔을 때 실행된다.
     * 1. Spring Security가 생성한 인증 요청 객체 (state, nonce 등 포함) 쿠키에 저장
     * 2. 클라이언트(Expo)가 파라미터로 보낸 redirect_uri
     */
    @Override
    public void saveAuthorizationRequest(OAuth2AuthorizationRequest authorizationRequest, HttpServletRequest request, HttpServletResponse response) {
        if (authorizationRequest == null) {
            removeAuthorizationRequestCookies(request, response);
            return;
        }

        // 1. Spring Security가 만든 인증 요청 객체를 직렬화해서 쿠키에 저장
        // (갔다 돌아왔을 때 위조된 요청인지 확인하기 위함)
        addCookie(response, OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME, serialize(authorizationRequest), cookieExpireSeconds);
        
        // 2. 앱에서 보낸 ?redirect_uri=... 값을 쿠키에 저장
        // (쿠키에 저장을 해야 나중에 SuccessHandler에서 앱으로 돌려보낼 때 같이 보내줌)
        String redirectUriAfterLogin = request.getParameter(REDIRECT_URI);
        if (StringUtils.hasText(redirectUriAfterLogin)) {
            addCookie(response, REDIRECT_URI, redirectUriAfterLogin, cookieExpireSeconds);
        }
    }

    /**
     * 인증 과정이 끝났을 때 쿠키에서 정보를 삭제하기 전, 정보를 반환합니다.
     */
    @Override
    public OAuth2AuthorizationRequest removeAuthorizationRequest(HttpServletRequest request, HttpServletResponse response) {
        return this.loadAuthorizationRequest(request);
    }
    
    /**
     * 사용 완료된 쿠키들을 모두 삭제합니다.
     */
    public void removeAuthorizationRequestCookies(HttpServletRequest request, HttpServletResponse response) {
        deleteCookie(request, response, OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME);
        deleteCookie(request, response, REDIRECT_URI);
    }

    // ================= [ Cookie Helper Methods ] =================

    // 요청에서 특정 이름의 쿠키 찾기
    private Optional<Cookie> getCookie(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null && cookies.length > 0) {
            for (Cookie cookie : cookies) {
                if (cookie.getName().equals(name)) {
                    return Optional.of(cookie);
                }
            }
        }
        return Optional.empty();
    }

    // ================= [ Cookie Helper Methods (수정됨) ] =================

    // 응답에 새 쿠키 추가 - ResponseCookie를 사용하여 SameSite 설정 적용 (CSRF 공격 방지)
    private void addCookie(HttpServletResponse response, String name, String value, int maxAge) {
        ResponseCookie cookie = ResponseCookie.from(name, value)
                .path("/")
                .httpOnly(true)
                .secure(false) // 개발환경(HTTP)이므로 false. 배포 시 true로 변경 필요!
                .sameSite("Lax") // [핵심] SameSite=Lax 설정
                .maxAge(maxAge)
                .build();

        // HttpServletResponse의 쿠키 메서드 대신 헤더에 직접 추가
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    // 쿠키 삭제 (만료시간을 0으로 설정)
    private void deleteCookie(HttpServletRequest request, HttpServletResponse response, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null && cookies.length > 0) {
            for (Cookie cookie : cookies) {
                if (cookie.getName().equals(name)) {
                    ResponseCookie deleteCookie = ResponseCookie.from(name, "")
                            .path("/")
                            .httpOnly(true)
                            .secure(false) // 개발환경이므로 false
                            .sameSite("Lax") // 생성 때와 동일하게 맞춰야 삭제됨
                            .maxAge(0) // 즉시 만료
                            .build();

                    response.addHeader(HttpHeaders.SET_COOKIE, deleteCookie.toString());
                }
            }
        }
    }

    /**
     * 객체를 Base64 문자열로 직렬화 (쿠키는 문자열만 저장 가능하므로)
     */
    private String serialize(Object object) {
        return Base64.getUrlEncoder()
                .encodeToString(SerializationUtils.serialize(object));
    }

    /**
     * Base64 문자열을 다시 객체로 역직렬화
     */
    private <T> T deserialize(Cookie cookie, Class<T> cls) {
        return cls.cast(SerializationUtils.deserialize(
                Base64.getUrlDecoder().decode(cookie.getValue())));
    }
}
