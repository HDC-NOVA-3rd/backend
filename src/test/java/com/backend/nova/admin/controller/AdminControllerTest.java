package com.backend.nova.admin.controller;

import com.backend.nova.auth.admin.AdminAuthenticationProvider;
import com.backend.nova.auth.jwt.JwtProvider;
import com.backend.nova.config.SecurityConfig;
import com.backend.nova.admin.dto.AdminLoginRequest;
import com.backend.nova.admin.dto.AdminLoginResponse;
import com.backend.nova.admin.service.AdminAuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminAuthController.class)
@Import(SecurityConfig.class)
class AdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminAuthService adminAuthService;

    @MockitoBean
    private JwtProvider jwtProvider;

    @MockitoBean
    private AdminAuthenticationProvider adminAuthenticationProvider;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("관리자 로그인 성공 테스트")
    void adminLogin_Success() throws Exception {
        // given
        AdminLoginRequest request = new AdminLoginRequest("admin", "password");

        AdminLoginResponse response = new AdminLoginResponse(
                1L,                        // adminId
                "admin",                   // name
                "admin-access-token",      // accessToken
                "admin-refresh-token"      // refreshToken
        );

        // 모든 Service/Provider 의존성을 MockitoBean으로 Mock 처리
        given(adminAuthService.login(any(AdminLoginRequest.class))).willReturn(response);

        // when & then
        mockMvc.perform(post("/api/admin/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.adminId").value(1))
                .andExpect(jsonPath("$.name").value("admin"))
                .andExpect(jsonPath("$.accessToken").value("admin-access-token"))
                .andExpect(jsonPath("$.refreshToken").value("admin-refresh-token"));
    }
}
