package com.backend.nova.auth.jwt;

import com.backend.nova.global.exception.ErrorCode;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.GenericFilterBean;

import java.io.IOException;

@RequiredArgsConstructor
public class JwtAuthenticationFilter extends GenericFilterBean {

    private final JwtProvider jwtProvider;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        // 1. Header 에서 Access 토큰 추출
        String accessToken = resolveToken((HttpServletRequest) request);
        try{
            // 2. 토큰 유효성 검사 후 인증객체 저장 (토큰이 맞는 값일 때 저장)
            if (accessToken != null && jwtProvider.validateToken(accessToken)) {
                Authentication authentication = jwtProvider.getAuthentication(accessToken);
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
            // 토큰에 문제가 있는 경우 아래 오류 발생
        } catch (ExpiredJwtException e){
            // Access 토큰이 만료된 경우
            request.setAttribute("exception", ErrorCode.ACCESS_TOKEN_EXPIRED); // 401
        } catch (JwtException | IllegalArgumentException e) {
            // 그 외의 여러가지 토큰 오류
            request.setAttribute("exception", ErrorCode.INVALID_TOKEN); //401
        }

        chain.doFilter(request, response);
    }

    // Request Header 에서 토큰 정보 추출
    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
