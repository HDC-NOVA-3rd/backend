package com.backend.nova.complaint.controller;

import com.backend.nova.admin.entity.*;
import com.backend.nova.admin.repository.AdminRepository;
import com.backend.nova.apartment.entity.*;
import com.backend.nova.apartment.repository.*;
import com.backend.nova.apartment.service.ApartmentWeatherService;
import com.backend.nova.auth.jwt.JwtProvider;
import com.backend.nova.auth.test.WithMockAdmin;
import com.backend.nova.auth.test.WithMockMember;
import com.backend.nova.complaint.dto.*;
import com.backend.nova.complaint.entity.*;
import com.backend.nova.complaint.repository.ComplaintRepository;
import com.backend.nova.member.entity.*;
import com.backend.nova.member.repository.MemberRepository;
import com.backend.nova.resident.entity.Resident;
import com.backend.nova.resident.repository.ResidentRepository;
import com.backend.nova.weather.service.OpenWeatherService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class ComplaintControllerIntegrationTest {

    private Long complaintId;
    private Long apartmentId;
    private Long targetAdminId;

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockBean JwtProvider jwtProvider;
    @MockBean ApartmentWeatherService apartmentWeatherService;
    @MockBean OpenWeatherService openWeatherService;

    @Autowired ApartmentRepository apartmentRepository;
    @Autowired DongRepository dongRepository;
    @Autowired HoRepository hoRepository;
    @Autowired ResidentRepository residentRepository;
    @Autowired MemberRepository memberRepository;
    @Autowired AdminRepository adminRepository;
    @Autowired ComplaintRepository complaintRepository;

    private String unique(String prefix) {
        return prefix + System.nanoTime();
    }

    @BeforeEach
    void setUp() {
        String u = UUID.randomUUID().toString().substring(0, 8);

        Apartment apartment = apartmentRepository.save(
                Apartment.builder()
                        .name("테스트 아파트")
                        .address("서울시 테스트구")
                        .latitude(37.5)
                        .longitude(127.0)
                        .build()
        );
        apartmentId = apartment.getId();

        Dong dong = dongRepository.save(
                Dong.builder()
                        .apartment(apartment)
                        .dongNo("101")
                        .build()
        );

        Ho ho = hoRepository.save(
                Ho.builder()
                        .dong(dong)
                        .hoNo("1001")
                        .floor(10)
                        .build()
        );

        Resident resident = residentRepository.save(
                Resident.builder()
                        .ho(ho)
                        .name("입주민")
                        .phone(unique("010"))
                        .build()
        );

        Member member = memberRepository.save(
                Member.builder()
                        .resident(resident)
                        .loginId("member_" + u)
                        .password("password")
                        .name("member")
                        .loginType(LoginType.NORMAL)
                        .email("member_" + u + "@test.com")
                        .build()
        );

        Admin admin1 = adminRepository.save(
                Admin.builder()
                        .apartment(apartment)
                        .loginId("admin1_" + u)
                        .name("관리자1")
                        .passwordHash("password")
                        .email("admin1_" + u + "@test.com")
                        .phoneNumber(unique("010"))
                        .role(AdminRole.MANAGER)
                        .status(AdminStatus.ACTIVE)
                        .build()
        );

        Admin admin2 = adminRepository.save(
                Admin.builder()
                        .apartment(apartment)
                        .loginId("admin2_" + u)
                        .name("관리자2")
                        .passwordHash("password")
                        .email("admin2_" + u + "@test.com")
                        .phoneNumber(unique("010"))
                        .role(AdminRole.SUPER_ADMIN)
                        .status(AdminStatus.ACTIVE)
                        .build()
        );
        targetAdminId = admin2.getId();

        Complaint complaint1 = complaintRepository.save(
                Complaint.builder()
                        .member(member)
                        .type(ComplaintType.MAINTENANCE)
                        .title("소음 민원")
                        .content("윗집이 시끄러워요")
                        .status(ComplaintStatus.RECEIVED) // ⭐ 핵심
                        .build()
        );


        Complaint complaint2 = complaintRepository.save(
                Complaint.builder()
                        .member(member)
                        .admin(admin1) // ⭐⭐⭐ 이 줄이 핵심
                        .type(ComplaintType.MAINTENANCE)
                        .title("배관 문제")
                        .content("화장실 배관에서 물이 새요.")
                        .status(ComplaintStatus.ASSIGNED) // 또는 RECEIVED + assign API 먼저 호출
                        .build()
        );

        complaintId = complaint2.getId();
    }

    // ================= MEMBER =================

    @Test
    @WithMockMember
    void createComplaint() throws Exception {
        mockMvc.perform(post("/api/complaint")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ComplaintCreateRequest(
                                        ComplaintType.MAINTENANCE,
                                        "배관 문제",
                                        "물 새요"
                                ))))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockMember
    void updateComplaint() throws Exception {
        mockMvc.perform(put("/api/complaint/{id}", complaintId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ComplaintUpdateRequest(
                                        ComplaintType.MAINTENANCE,
                                        "수정",
                                        "수정 내용"
                                ))))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockMember
    void deleteComplaint() throws Exception {
        mockMvc.perform(delete("/api/complaint/{id}", complaintId))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockMember
    void createFeedback() throws Exception {
        mockMvc.perform(post("/api/complaint/{id}/feedbacks", complaintId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ComplaintFeedbackCreateRequest("좋아요", BigDecimal.valueOf(5))
                        )))
                .andExpect(status().isOk());
    }

    // ================= ADMIN =================

    @Test
    @WithMockAdmin
    void getComplaintsByApartment() throws Exception {
        mockMvc.perform(get("/api/complaint/apartment/{id}", apartmentId))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockAdmin
    void changeStatus() throws Exception {
        mockMvc.perform(post("/api/complaint/{id}/status", complaintId)
                        .param("status", ComplaintStatus.ASSIGNED.name()))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockAdmin
    void completeComplaint() throws Exception {
        mockMvc.perform(post("/api/complaint/{id}/complete", complaintId))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockAdmin
    void assignAdmin() throws Exception {
        mockMvc.perform(post("/api/complaint/{id}/assign", complaintId)
                        .param("targetAdminId", targetAdminId.toString()))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockAdmin
    void createAnswer() throws Exception {
        mockMvc.perform(post("/api/complaint/{id}/answers", complaintId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ComplaintAnswerCreateRequest("처리 완료")
                        )))
                .andExpect(status().isOk());
    }
}
