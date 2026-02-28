package com.backend.nova.oauth2.handler;

import com.backend.nova.oauth2.repository.OAuthRedirectCookieRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuthFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    private final OAuthRedirectCookieRepository oAuthRedirectCookieRepository;

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) throws IOException, ServletException {
        log.error("소셜 로그인 실패: {}", exception.getMessage());

        String targetUri = getRedirectUri(request);

        // 실패했다는 표시(error)를 달아서 앱으로 돌려보냄
        String targetUrl = UriComponentsBuilder.fromUriString(targetUri)
                .queryParam("status", "FAIL")
                .queryParam("error", exception.getMessage())
                .build().toUriString();

        // 쿠키 삭제
        oAuthRedirectCookieRepository.removeAuthorizationRequestCookies(request, response);

        // 리다이렉트
        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }

    // 쿠키에서 redirect_uri 찾는 메서드 (SuccessHandler와 동일)
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