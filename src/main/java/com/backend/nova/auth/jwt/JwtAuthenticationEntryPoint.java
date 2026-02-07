package com.backend.nova.auth.jwt;

import com.backend.nova.global.exception.ErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {
    private final ObjectMapper objectMapper = new ObjectMapper();
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException {
        // 1. JSON 응답 설정
        response.setContentType("application/json;charset=UTF-8");
        response.setStatus(ErrorCode.UNAUTHORIZED.getStatus().value()); // 401 Status

        // 2. 응답 Body 데이터 구성
        Map<String, Object> errorBody = new HashMap<>();
        errorBody.put("code", ErrorCode.UNAUTHORIZED.name());
        errorBody.put("message", ErrorCode.UNAUTHORIZED.getMessage());

        // 3. 출력
        response.getWriter().write(objectMapper.writeValueAsString(errorBody));
    }
}
