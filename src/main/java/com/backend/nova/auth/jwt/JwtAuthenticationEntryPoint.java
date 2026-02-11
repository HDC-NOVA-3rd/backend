package com.backend.nova.auth.jwt;

import com.backend.nova.global.exception.ErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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
        // 1. Filter에서 넣어둔 에러 코드 꺼내기
        Object exception = request.getAttribute("exception");
        ErrorCode errorCode;
        if (exception instanceof ErrorCode) {
            errorCode = (ErrorCode) exception;
        } else {
            // Filter를 통과했는데 인증이 안 된 경우 (예: 헤더에 토큰이 아예 없음)
            errorCode = ErrorCode.UNAUTHORIZED; // 401
        }

        // 2. JSON 응답 설정
        response.setContentType("application/json;charset=UTF-8");
        response.setStatus(errorCode.getStatus().value()); // filter에서 에러나면 ALl 401 Status

        // 3. 응답 Body 데이터 구성
        Map<String, Object> errorBody = new HashMap<>();
        errorBody.put("code", errorCode.name());
        errorBody.put("message", errorCode.getMessage());

        // 4. 출력
        response.getWriter().write(objectMapper.writeValueAsString(errorBody));
    }
}
