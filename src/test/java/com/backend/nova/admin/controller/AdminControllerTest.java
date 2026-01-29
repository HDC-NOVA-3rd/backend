package com.backend.nova.admin.controller;

import com.backend.nova.admin.dto.AdminLoginRequest;
import com.backend.nova.admin.entity.Admin;
import com.backend.nova.admin.repository.AdminRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb;MODE=MySQL",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",

        // 32바이트 이상 + Base64
        "jwt.secret=MzJieXRlLXNlY3JldC1rZXktZm9yLWp3dC10ZXN0LSEhISE=",
        "jwt.access-token-expire-time=3600000",
        "jwt.refresh-token-expire-time=604800000"
})


class AdminControllerIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    AdminRepository adminRepository;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Test
    @DisplayName("관리자 로그인 통합 테스트 - 성공")
    void adminLogin_success() throws Exception {
        // given
        Admin admin = Admin.builder()
                .loginId("admin-" + UUID.randomUUID())   // UNIQUE 안전
                .passwordHash(passwordEncoder.encode("1234")) //  필드명 정확
                .name("테스트 관리자")
                .email("admin-" + UUID.randomUUID() + "@test.com") //  UNIQUE
                .apartmentId("APT-TEST")                 //  NOT NULL
                // status, role, createdAt 등은 @PrePersist가 처리
                .build();


        adminRepository.save(admin);

        AdminLoginRequest request =
                new AdminLoginRequest("admin", "1234");

        // when & then
        mockMvc.perform(post("/api/admin/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())

                // === 핵심 필드 검증 ===
                .andExpect(jsonPath("$.adminId").isNumber())
                .andExpect(jsonPath("$.name").isNotEmpty())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())

                // refreshToken은 선택이니까 존재만 체크
                .andExpect(jsonPath("$.refreshToken").exists());

//        MvcResult result = mockMvc.perform(post("/api/admin/login")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(request)))
//                //.andDo(print())   // json출력
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.adminId").isNumber())
//                .andExpect(jsonPath("$.accessToken").isNotEmpty())
//                .andExpect(jsonPath("$.refreshToken").isNotEmpty()).andReturn();

        //GitHub Actions 로그출력
//        System.out.println("RESPONSE BODY = " +
//                result.getResponse().getContentAsString());
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
