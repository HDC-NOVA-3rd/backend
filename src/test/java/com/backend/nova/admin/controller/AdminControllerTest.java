package com.backend.nova.admin.controller;

import com.backend.nova.auth.admin.AdminAuthenticationProvider;
import com.backend.nova.auth.jwt.JwtProvider;
import com.backend.nova.config.SecurityConfig;
import com.backend.nova.admin.dto.*;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;

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

    // ----------------- 관리자 로그인 -----------------
    @Test
    @DisplayName("관리자 로그인 성공 테스트")
    void adminLogin_Success() throws Exception {
        AdminLoginRequest request = new AdminLoginRequest("admin", "password");
        AdminLoginResponse response = new AdminLoginResponse(
                1L, "admin", "admin-access-token", "admin-refresh-token"
        );

        given(adminAuthService.login(any(AdminLoginRequest.class)))
                .willReturn(response);

        mockMvc.perform(post("/api/admin/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.adminId").value(1))
                .andExpect(jsonPath("$.name").value("admin"))
                .andExpect(jsonPath("$.accessToken").value("admin-access-token"))
                .andExpect(jsonPath("$.refreshToken").value("admin-refresh-token"));
    }

    // ----------------- 관리자 생성 -----------------
//    @Test
//    @DisplayName("관리자 생성 성공 테스트")
//    void createAdmin_Success() throws Exception {
//        AdminCreateRequest request = new AdminCreateRequest(
//                "newAdmin", "password", "테스트 관리자", "newadmin@test.com", null
//        );
//
//        given(adminAuthService.createAdmin(any(AdminCreateRequest.class)))
//                .willReturn(2L);
//
//        mockMvc.perform(post("/api/admin")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(request)))
//                .andExpect(status().isCreated())
//                .andExpect(header().string("Location", "/api/admin/2"));
//    }

    // ----------------- OTP 검증 -----------------
//    @Test
//    @DisplayName("OTP 검증 성공 테스트")
//    void verifyOtp_Success() throws Exception {
//        AdminOtpVerifyRequest request = new AdminOtpVerifyRequest("admin", "123456");
//
//        given(adminAuthService.verifyLoginOtp(any(AdminLoginOtpVerifyRequest.class)))
//                .willReturn(true);
//
//        mockMvc.perform(post("/api/admin/login/verify-otp")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(request)))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.verified").value(true));
//    }
//
//    @Test
//    @DisplayName("OTP 검증 실패 테스트")
//    void verifyOtp_Fail() throws Exception {
//        AdminOtpVerifyRequest request = new AdminOtpVerifyRequest("admin", "000000");
//
//        given(adminAuthService.verifyLoginOtp(any(AdminLoginOtpVerifyRequest.class)))
//                .willReturn(false);
//
//        mockMvc.perform(post("/api/admin/login/verify-otp")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(request)))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.verified").value(false));
//    }
}
