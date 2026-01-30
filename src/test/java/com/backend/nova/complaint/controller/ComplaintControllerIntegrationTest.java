package com.backend.nova.complaint.controller;

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
import com.backend.nova.apartment.service.ApartmentWeatherService;
import com.backend.nova.auth.admin.AdminDetails;
import com.backend.nova.auth.jwt.JwtProvider;
import com.backend.nova.auth.member.MemberDetails;
import com.backend.nova.auth.test.WithMockAdmin;
import com.backend.nova.auth.test.WithMockMember;
import com.backend.nova.complaint.dto.*;
import com.backend.nova.complaint.entity.Complaint;
import com.backend.nova.complaint.entity.ComplaintStatus;
import com.backend.nova.complaint.entity.ComplaintType;
import com.backend.nova.complaint.repository.ComplaintRepository;
import com.backend.nova.member.entity.LoginType;
import com.backend.nova.member.entity.Member;
import com.backend.nova.member.repository.MemberRepository;
import com.backend.nova.resident.entity.Resident;
import com.backend.nova.resident.repository.ResidentRepository;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.context.support.WithSecurityContext;
import org.springframework.security.test.context.support.WithSecurityContextFactory;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;


import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class ComplaintControllerIntegrationTest {

    private Long memberId;
    private Long complaintId;
    private Long adminId;
    private Long targetAdminId;
    private Long apartmentId;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private JwtProvider jwtProvider;

    @MockBean
    private ApartmentWeatherService apartmentWeatherService;

    @MockBean
    private OpenWeatherService openWeatherService;

    @Autowired
    private ApartmentRepository apartmentRepository;
    @Autowired
    private DongRepository dongRepository;
    @Autowired
    private HoRepository hoRepository;
    @Autowired
    private ResidentRepository residentRepository;
    @Autowired
    private MemberRepository memberRepository;
    @Autowired
    private AdminRepository adminRepository;
    @Autowired
    private ComplaintRepository complaintRepository;

    private String unique(String prefix) {
        return prefix + System.nanoTime();
    }

    @BeforeEach
    void setUp() {
        String u = UUID.randomUUID().toString().substring(0, 8);

        // Apartment
        Apartment apartment = apartmentRepository.save(
                Apartment.builder()
                        .name("테스트 아파트")
                        .address("서울시 테스트구")
                        .latitude(37.5)
                        .longitude(127.0)
                        .build()
        );
        this.apartmentId = apartment.getId();

        // Dong
        Dong dong = dongRepository.save(
                Dong.builder()
                        .apartment(apartment)
                        .dongNo("101")
                        .build()
        );

        // Ho
        Ho ho = hoRepository.save(
                Ho.builder()
                        .dong(dong)
                        .hoNo("1001")
                        .floor(10)
                        .build()
        );

        // Resident
        Resident resident = residentRepository.save(
                Resident.builder()
                        .ho(ho)
                        .name("입주민1")
                        .phone(unique("010"))
                        .build()
        );

        // Member
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
        this.memberId = member.getId();

        // Admin 1
        Admin admin1 = adminRepository.save(
                Admin.builder()
                        .apartment(apartment)
                        .loginId("admin1_" + u)
                        .name("관리자1")
                        .passwordHash("password")
                        .email("admin1_" + u + "@test.com")
                        .phoneNumber(unique("010"))
                        .build()
        );
        this.adminId = admin1.getId();

        // Admin 2
        Admin admin2 = adminRepository.save(
                Admin.builder()
                        .apartment(apartment)
                        .loginId("admin2_" + u)
                        .name("관리자2")
                        .passwordHash("password")
                        .email("admin2_" + u + "@test.com")
                        .phoneNumber(unique("010"))
                        .build()
        );
        this.targetAdminId = admin2.getId();

        // Complaint
        Complaint complaint = complaintRepository.save(
                Complaint.builder()
                        .member(member)
                        .type(ComplaintType.MAINTENANCE)
                        .title("배관 문제")
                        .content("화장실 배관에서 물이 새요.")
                        .status(ComplaintStatus.RECEIVED)
                        .build()
        );
        this.complaintId = complaint.getId();
    }

    // ======================== MEMBER ========================

    @Test
    @DisplayName("입주민 민원 등록 테스트")
    @WithMockMember
    void createComplaint() throws Exception {
        ComplaintCreateRequest request = new ComplaintCreateRequest(
                ComplaintType.MAINTENANCE,
                "배관 문제",
                "화장실 배관에서 물이 새요."
        );

        mockMvc.perform(post("/api/complaint")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("입주민 민원 수정 테스트")
    @WithMockMember
    void updateComplaint() throws Exception {
        ComplaintUpdateRequest request = new ComplaintUpdateRequest(
                ComplaintType.MAINTENANCE,
                "수정 제목",
                "수정 내용"
        );

        mockMvc.perform(put("/api/complaint/{id}", complaintId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("입주민 민원 삭제 테스트")
    @WithMockMember
    void deleteComplaint() throws Exception {
        mockMvc.perform(delete("/api/complaint/{id}", complaintId))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("입주민 민원 피드백 등록 테스트")
    @WithMockMember
    void createFeedback() throws Exception {
        ComplaintFeedbackCreateRequest request =
                new ComplaintFeedbackCreateRequest("좋아요", BigDecimal.valueOf(5));

        mockMvc.perform(post("/api/complaint/{id}/feedbacks", complaintId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    // ======================== ADMIN ========================

    @Test
    @DisplayName("관리자 아파트별 민원 조회")
    @WithMockAdmin
    void getComplaintsByApartment() throws Exception {
        mockMvc.perform(get("/api/complaint/apartment/{id}", apartmentId))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("관리자 민원 상태 변경")
    @WithMockAdmin
    void changeStatus() throws Exception {
        mockMvc.perform(post("/api/complaint/{id}/status", complaintId)
                        .param("status", ComplaintStatus.ASSIGNED.name()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("관리자 민원 완료")
    @WithMockAdmin
    void completeComplaint() throws Exception {
        mockMvc.perform(post("/api/complaint/{id}/complete", complaintId))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("관리자 민원 담당자 배정")
    @WithMockAdmin
    void assignAdmin() throws Exception {
        mockMvc.perform(post("/api/complaint/{id}/assign", complaintId)
                        .param("targetAdminId", targetAdminId.toString()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("관리자 민원 답변 등록")
    @WithMockAdmin
    void createAnswer() throws Exception {
        ComplaintAnswerCreateRequest request =
                new ComplaintAnswerCreateRequest("처리 완료");

        mockMvc.perform(post("/api/complaint/{id}/answers", complaintId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }
}
