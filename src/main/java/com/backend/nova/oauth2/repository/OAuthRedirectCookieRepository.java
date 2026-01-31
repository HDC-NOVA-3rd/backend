package com.backend.nova.oauth2.repository;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.SerializationUtils;
import org.springframework.util.StringUtils;

import java.util.Base64;
import java.util.Optional;

/**
 * [OAuth2 인증 요청 및 Redirect URI 보존을 위한 쿠키 기반 레포지토리]
 * 
 * 왜 필요한가요?
 * 1. OAuth2 인증은 [앱 -> 서버 -> 소셜로그인창 -> 서버 -> 앱]의 여러 단계를 거칩니다.
 * 2. 이 과정에서 서버는 "나중에 앱의 어디로 돌아가야 하는지(redirect_uri)"를 기억해야 합니다.
 * 3. 우리 서버는 Stateless(JWT) 방식이므로 세션을 쓰지 않기 때문에, 이 정보를 브라우저 쿠키에 잠시 저장해둡니다.
 */
@Component
public class OAuthRedirectCookieRepository implements AuthorizationRequestRepository<OAuth2AuthorizationRequest> {

    // OAuth2 인증 요청 정보를 담을 쿠키 이름
    public static final String OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME = "oauth2_auth_request";
    // 앱에서 파라미터로 보낸 redirect_uri를 담을 쿠키 이름
    public static final String REDIRECT_URI_PARAM_COOKIE_NAME = "redirect_uri";
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
     * 최초 인증 요청 시, 필요한 정보들을 쿠키에 저장합니다.
     * 1. Spring Security가 생성한 인증 요청 객체 (state, nonce 등 포함)
     * 2. 클라이언트(Expo)가 파라미터로 보낸 redirect_uri
     */
    @Override
    public void saveAuthorizationRequest(OAuth2AuthorizationRequest authorizationRequest, HttpServletRequest request, HttpServletResponse response) {
        if (authorizationRequest == null) {
            removeAuthorizationRequestCookies(request, response);
            return;
        }

        // 1. 인증 요청 객체를 직렬화해서 쿠키에 저장
        addCookie(response, OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME, serialize(authorizationRequest), cookieExpireSeconds);
        
        // 2. 앱에서 보낸 ?redirect_uri=... 값을 쿠키에 저장 (이게 있어야 나중에 SuccessHandler에서 앱으로 돌려보냄)
        String redirectUriAfterLogin = request.getParameter(REDIRECT_URI_PARAM_COOKIE_NAME);
        if (StringUtils.hasText(redirectUriAfterLogin)) {
            addCookie(response, REDIRECT_URI_PARAM_COOKIE_NAME, redirectUriAfterLogin, cookieExpireSeconds);
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
        deleteCookie(request, response, REDIRECT_URI_PARAM_COOKIE_NAME);
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

    // 응답에 새 쿠키 추가
    private void addCookie(HttpServletResponse response, String name, String value, int maxAge) {
        Cookie cookie = new Cookie(name, value);
        cookie.setPath("/");
        cookie.setHttpOnly(true); // HttpOnly Cookie 설정
        cookie.setMaxAge(maxAge);
        response.addCookie(cookie);
    }

    // 쿠키 삭제 (만료시간을 0으로 설정)
    private void deleteCookie(HttpServletRequest request, HttpServletResponse response, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null && cookies.length > 0) {
            for (Cookie cookie : cookies) {
                if (cookie.getName().equals(name)) {
                    cookie.setValue("");
                    cookie.setPath("/");
                    cookie.setMaxAge(0);
                    response.addCookie(cookie);
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
