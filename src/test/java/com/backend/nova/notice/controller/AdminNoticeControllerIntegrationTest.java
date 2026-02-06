package com.backend.nova.notice.controller;

import com.backend.nova.admin.entity.Admin;
import com.backend.nova.admin.entity.AdminRole;
import com.backend.nova.admin.entity.AdminStatus;
import com.backend.nova.admin.repository.AdminRepository;
import com.backend.nova.apartment.entity.Apartment;
import com.backend.nova.apartment.entity.Dong;
import com.backend.nova.apartment.entity.Ho;
import com.backend.nova.apartment.repository.ApartmentRepository;
import com.backend.nova.apartment.repository.DongRepository;
import com.backend.nova.apartment.repository.HoRepository;
import com.backend.nova.notice.dto.NoticeCreateRequest;
import com.backend.nova.notice.dto.NoticeCreateResponse;
import com.backend.nova.notice.dto.NoticeSendRequest;
import com.backend.nova.notice.repository.NoticeRepository;
import com.backend.nova.notice.repository.NoticeSendLogRepository;
import com.backend.nova.notice.repository.NoticeTargetDongRepository;
import com.backend.nova.resident.entity.Resident;
import com.backend.nova.resident.repository.ResidentRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.show-sql=false",
        "jwt.secret=MzJieXRlLXNlY3JldC1rZXktZm9yLWp3dC10ZXN0LSEhISE=",
        "jwt.access-token-expire-time=3600000",
        "jwt.refresh-token-expire-time=604800000"
})
class AdminNoticeControllerIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    AdminRepository adminRepository;

    @Autowired
    ApartmentRepository apartmentRepository;

    @Autowired
    DongRepository dongRepository;

    @Autowired
    HoRepository hoRepository;

    @Autowired
    ResidentRepository residentRepository;

    @Autowired
    NoticeRepository noticeRepository;

    @Autowired
    NoticeSendLogRepository noticeSendLogRepository;

    @Autowired
    NoticeTargetDongRepository noticeTargetDongRepository;

    @BeforeEach
    void cleanDb() {
        noticeSendLogRepository.deleteAllInBatch();
        noticeTargetDongRepository.deleteAllInBatch();
        noticeRepository.deleteAllInBatch();
        residentRepository.deleteAllInBatch();
        hoRepository.deleteAllInBatch();
        dongRepository.deleteAllInBatch();
        adminRepository.deleteAllInBatch();
        apartmentRepository.deleteAllInBatch();
    }

    @Test
    @DisplayName("관리자 공지 등록/발송/로그 조회 통합 테스트")
    void noticeFlow_success() throws Exception {
        Apartment apartment = createApartment();
        Admin admin = createAdmin(apartment);
        Ho ho = createHo(createDong(apartment));
        createResident(ho, "김영희", "010-0000-0001");
        createResident(ho, "이영희", "010-0000-0002");

        NoticeCreateRequest createRequest = new NoticeCreateRequest("정기 소독 안내", "다음주 화요일 소독 예정", null);

        String createResponseJson = mockMvc.perform(post("/api/admin/notice")
                        .with(user(admin.getId().toString()).roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.noticeId").isNumber())
                .andReturn()
                .getResponse()
                .getContentAsString();

        NoticeCreateResponse createResponse = objectMapper.readValue(createResponseJson, NoticeCreateResponse.class);

        NoticeSendRequest sendRequest = new NoticeSendRequest(null);

        mockMvc.perform(post("/api/admin/notice/{noticeId}/send-alert", createResponse.noticeId())
                        .with(user(admin.getId().toString()).roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sendRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.sentCount").value(2));

        mockMvc.perform(get("/api/admin/notice/log")
                        .with(user(admin.getId().toString()).roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].type").value("notice"));
    }

    @Test
    @DisplayName("공지 전송 - 공지 없음")
    void sendNotice_notFound() throws Exception {
        Apartment apartment = createApartment();
        Admin admin = createAdmin(apartment);

        NoticeSendRequest sendRequest = new NoticeSendRequest(null);

        mockMvc.perform(post("/api/admin/notice/{noticeId}/send-alert", 999L)
                        .with(user(admin.getId().toString()).roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sendRequest)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOTICE_NOT_FOUND"));
    }

    private Apartment createApartment() {
        Apartment apartment = Apartment.builder()
                .name("테스트 아파트-" + UUID.randomUUID())
                .address("서울시 테스트구 테스트동")
                .latitude(37.5665)
                .longitude(126.9780)
                .build();
        return apartmentRepository.saveAndFlush(apartment);
    }

    private Dong createDong(Apartment apartment) {
        Dong dong = Dong.builder()
                .apartment(apartment)
                .dongNo("101")
                .build();
        return dongRepository.saveAndFlush(dong);
    }

    private Ho createHo(Dong dong) {
        Ho ho = Ho.builder()
                .dong(dong)
                .hoNo("1001")
                .floor(10)
                .build();
        return hoRepository.saveAndFlush(ho);
    }

    private Resident createResident(Ho ho, String name, String phone) {
        Resident resident = Resident.builder()
                .ho(ho)
                .name(name)
                .phone(phone)
                .build();
        return residentRepository.saveAndFlush(resident);
    }

    private Admin createAdmin(Apartment apartment) {
        String uuid = UUID.randomUUID().toString();
        Admin admin = Admin.builder()
                .loginId("admin-" + uuid)
                .email("admin-" + uuid + "@test.com")
                .passwordHash("pw")
                .name("테스트 관리자")
                .role(AdminRole.ADMIN)
                .status(AdminStatus.ACTIVE)
                .failedLoginCount(0)
                .apartment(apartment)
                .build();
        return adminRepository.saveAndFlush(admin);
    }

    @Test
    @DisplayName("공지 전송 - 동 단위 전송")
    void sendNotice_byDong() throws Exception {
        Apartment apartment = createApartment();
        Admin admin = createAdmin(apartment);
        Dong dong = createDong(apartment);
        Ho ho = createHo(dong);
        createResident(ho, "김영희", "010-0000-0001");
        createResident(ho, "이영희", "010-0000-0002");

        NoticeCreateRequest createRequest = new NoticeCreateRequest("공지", "동 공지", List.of(dong.getId()));
        String createResponseJson = mockMvc.perform(post("/api/admin/notice")
                        .with(user(admin.getId().toString()).roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        NoticeCreateResponse createResponse = objectMapper.readValue(createResponseJson, NoticeCreateResponse.class);

        NoticeSendRequest sendRequest = new NoticeSendRequest(List.of(dong.getId()));

        mockMvc.perform(post("/api/admin/notice/{noticeId}/send-alert", createResponse.noticeId())
                        .with(user(admin.getId().toString()).roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sendRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.sentCount").value(2));
    }
}
