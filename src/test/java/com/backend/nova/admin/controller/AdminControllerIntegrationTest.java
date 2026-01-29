package com.backend.nova.admin.controller;

import com.backend.nova.admin.dto.AdminLoginRequest;
import com.backend.nova.admin.entity.Admin;
import com.backend.nova.admin.entity.AdminRole;
import com.backend.nova.admin.entity.AdminStatus;
import com.backend.nova.admin.repository.AdminRepository;
import com.backend.nova.apartment.entity.Apartment;
import com.backend.nova.apartment.repository.ApartmentRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
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
    ApartmentRepository apartmentRepository;

    @Autowired
    PasswordEncoder passwordEncoder;

    /**
     * 테스트 시작 전 DB 정리
     */
    @BeforeEach
    void clean() {
        adminRepository.deleteAll();
        apartmentRepository.deleteAll();
    }

    /**
     * 테스트용 Apartment 생성
     */
    private Apartment createApartment() {
        Apartment apartment = Apartment.builder()
                .name("테스트 아파트")
                .address("서울시 테스트구 테스트동") //  필수
                .build();
        return apartmentRepository.save(apartment);
    }

    @Test
    @DisplayName("관리자 로그인 통합 테스트 - 성공")
    void adminLogin_success() throws Exception {
        // given
        String loginId = "admin-" + UUID.randomUUID();

        Apartment apartment = createApartment();

        Admin admin = Admin.builder()
                .loginId(loginId)
                .passwordHash(passwordEncoder.encode("1234"))
                .name("테스트 관리자")
                .email("admin-" + UUID.randomUUID() + "@test.com")
                .role(AdminRole.ADMIN)
                .status(AdminStatus.ACTIVE)
                .apartment(apartment)
                .build();

        adminRepository.save(admin);

        AdminLoginRequest request =
                new AdminLoginRequest(loginId, "1234");

        // when & then
        mockMvc.perform(post("/api/admin/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())

                // === 핵심 필드 검증 ===
                .andExpect(jsonPath("$.adminId").isNumber())
                .andExpect(jsonPath("$.name").isNotEmpty())
                .andExpect(jsonPath("$.accessToken").isNotEmpty());

        // refreshToken은 선택이니까 존재만 체크
        //.andExpect(jsonPath("$.refreshToken").exists());
    }

    // 관리자 로그인 실패 – 비밀번호 불일치
    @Test
    @DisplayName("관리자 로그인 실패 - 비밀번호 불일치")
    void adminLogin_fail_wrongPassword() throws Exception {
        // given
        Apartment apartment = createApartment();

        Admin admin = Admin.builder()
                .loginId("admin-" + UUID.randomUUID())
                .passwordHash(passwordEncoder.encode("1234"))
                .name("테스트 관리자")
                .email("admin-" + UUID.randomUUID() + "@test.com")
                .role(AdminRole.ADMIN)
                .status(AdminStatus.ACTIVE)
                .apartment(apartment)
                .build();

        adminRepository.save(admin);

        AdminLoginRequest request =
                new AdminLoginRequest(admin.getLoginId(), "wrong-password");

        // when & then
        mockMvc.perform(post("/api/admin/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").exists());
    }

    // 관리자 로그인 실패 – 존재하지 않는 ID
    @Test
    @DisplayName("관리자 로그인 실패 - 존재하지 않는 관리자")
    void adminLogin_fail_notFound() throws Exception {
        // given
        AdminLoginRequest request =
                new AdminLoginRequest("not-exist-admin", "1234");

        // when & then
        mockMvc.perform(post("/api/admin/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").exists());
    }

    // 요청값 검증 실패 – loginId 누락 (@Valid)
    @Test
    @DisplayName("관리자 로그인 실패 - 요청값 검증 오류")
    void adminLogin_fail_validation() throws Exception {
        // given
        String invalidJson = """
        {
          "password": "1234"
        }
        """;

        // when & then
        mockMvc.perform(post("/api/admin/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }
}
