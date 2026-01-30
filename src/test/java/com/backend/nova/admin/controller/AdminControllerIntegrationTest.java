package com.backend.nova.admin.controller;

import com.backend.nova.admin.dto.AdminLoginRequest;
import com.backend.nova.admin.entity.Admin;
import com.backend.nova.admin.entity.AdminRole;
import com.backend.nova.admin.entity.AdminStatus;
import com.backend.nova.admin.repository.AdminRepository;
import com.backend.nova.apartment.controller.ApartmentWeatherController;
import com.backend.nova.apartment.entity.Apartment;
import com.backend.nova.apartment.repository.ApartmentRepository;
import com.backend.nova.weather.service.OpenWeatherService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.show-sql=true",                               // SQL 출력
        "spring.jpa.properties.hibernate.format_sql=true",        // 보기 좋게 포맷
        "logging.level.org.hibernate.SQL=DEBUG",                 // Hibernate SQL 로그
        "logging.level.org.hibernate.type.descriptor.sql=TRACE", // 바인딩 값 출력
        "jwt.secret=MzJieXRlLXNlY3JldC1rZXktZm9yLWp3dC10ZXN0LSEhISE=",
        "jwt.access-token-expire-time=3600000",
        "jwt.refresh-token-expire-time=604800000"
})


class AdminControllerIntegrationTest {

    @MockBean
    private OpenWeatherService openWeatherService; // 실제 API 호출 막기

    @Autowired
    private ApartmentWeatherController controller;

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

    @BeforeEach
    void cleanDb() {
        adminRepository.deleteAllInBatch();      // delete Admins first
        apartmentRepository.deleteAllInBatch();  // then delete Apartments
        System.out.println("===== 테스트 DB 초기화 완료 =====");
    }



    /** 새로운 Apartment 생성 */
    private Apartment createApartment() {
        Apartment apartment = Apartment.builder()
                .name("테스트 아파트-" + UUID.randomUUID())
                .address("서울시 테스트구 테스트동")
                .latitude(37.5665)     // 테스트용 위도
                .longitude(126.9780)   // 테스트용 경도
                .build();
        return apartmentRepository.saveAndFlush(apartment);
    }


    /** 새로운 Admin 생성 (UNIQUE 안전) */
    private Admin createAdminSafe() {
        Apartment apartment = createApartment();
        String uuid = UUID.randomUUID().toString();
        Admin admin = Admin.builder()
                .loginId("admin-" + uuid)
                .email("admin-" + uuid + "@test.com")
                .passwordHash(passwordEncoder.encode("12345678"))
                .name("테스트 관리자")
                .role(AdminRole.ADMIN)
                .status(AdminStatus.ACTIVE)
                .failedLoginCount(0)
                .apartment(apartment)
                .build();

        admin = adminRepository.saveAndFlush(admin);

        // 콘솔 출력
        System.out.println("===== Admin 생성 =====");
        System.out.println("loginId: " + admin.getLoginId());
        System.out.println("email: " + admin.getEmail());
        System.out.println("apartmentId: " + apartment.getId());
        System.out.println("======================");

        return admin;
    }


    @Test
    @DisplayName("관리자 로그인 통합 테스트 - 성공")
    void adminLogin_success() throws Exception {
        Admin admin = createAdminSafe();
        AdminLoginRequest request = new AdminLoginRequest(admin.getLoginId(), "12345678");

        mockMvc.perform(post("/api/admin/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.adminId").isNumber())
                .andExpect(jsonPath("$.name").isNotEmpty())
                .andExpect(jsonPath("$.accessToken").isNotEmpty());
    }

    @Test
    @DisplayName("관리자 로그인 실패 - 비밀번호 불일치")
    void adminLogin_fail_wrongPassword() throws Exception {
        Admin admin = createAdminSafe();
        AdminLoginRequest request = new AdminLoginRequest(admin.getLoginId(), "wrong-password");

        mockMvc.perform(post("/api/admin/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @DisplayName("관리자 로그인 실패 - 존재하지 않는 관리자")
    void adminLogin_fail_notFound() throws Exception {
        AdminLoginRequest request = new AdminLoginRequest("not-exist-admin", "12345678");

        mockMvc.perform(post("/api/admin/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @DisplayName("관리자 로그인 실패 - 요청값 검증 오류")
    void adminLogin_fail_validation() throws Exception {
        String invalidJson = """
        {
          "password": "1234"
        }
        """;

        mockMvc.perform(post("/api/admin/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
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
