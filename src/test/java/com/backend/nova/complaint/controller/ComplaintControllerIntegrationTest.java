package com.backend.nova.complaint.controller;

import com.backend.nova.admin.entity.*;
import com.backend.nova.admin.repository.AdminRepository;
import com.backend.nova.apartment.entity.*;
import com.backend.nova.apartment.repository.*;
import com.backend.nova.apartment.service.ApartmentWeatherService;
import com.backend.nova.auth.admin.AdminDetails;
import com.backend.nova.auth.jwt.JwtProvider;
import com.backend.nova.auth.member.MemberDetails;
import com.backend.nova.complaint.dto.*;
import com.backend.nova.complaint.entity.*;
import com.backend.nova.complaint.repository.ComplaintFeedbackRepository;
import com.backend.nova.complaint.repository.ComplaintRepository;
import com.backend.nova.member.entity.*;
import com.backend.nova.member.repository.MemberRepository;
import com.backend.nova.resident.entity.Resident;
import com.backend.nova.resident.repository.ResidentRepository;
import com.backend.nova.weather.service.OpenWeatherService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

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

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

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
    @Autowired
    ComplaintFeedbackRepository complaintFeedbackRepository;


    private String unique(String prefix) {
        return prefix + System.nanoTime();
    }

    private Admin admin1;
    private Admin admin2;
    private Member member;

    @BeforeEach
    void setUp() {
        String u = UUID.randomUUID().toString().substring(0, 8);

        // ---------- 아파트/입주민/회원/관리자/민원 데이터 생성 ----------
        Apartment apartment = apartmentRepository.save(
                Apartment.builder()
                        .name("테스트 아파트")
                        .address("서울시 테스트구")
                        .latitude(37.5)
                        .longitude(127.0)
                        .build()
        );
        apartmentId = apartment.getId();


        Dong dong = dongRepository.save(Dong.builder().apartment(apartment).dongNo("101").build());
        Ho ho = hoRepository.save(Ho.builder().dong(dong).hoNo("1001").floor(10).build());
        Resident resident = residentRepository.save(
                Resident.builder().ho(ho).name("입주민").phone(unique("010")).build()
        );

        member = memberRepository.save(
                Member.builder()
                        .resident(resident)
                        .loginId("member_" + u)
                        .password("password")
                        .name("member")
                        .loginType(LoginType.NORMAL)
                        .email("member_" + u + "@test.com")
                        .build()
        );

        admin1 = adminRepository.save(
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
        admin2 = adminRepository.save(
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
                        .status(ComplaintStatus.RECEIVED)
                        .build()
        );
        Complaint complaint2 = complaintRepository.save(
                Complaint.builder()
                        .member(member)
                        .admin(admin1) // 여기 추가!
                        .type(ComplaintType.MAINTENANCE)
                        .title("배관 문제")
                        .content("화장실 배관에서 물이 새요.")
                        .status(ComplaintStatus.RECEIVED)
                        .build()
        );

        complaintId = complaint2.getId();

        // ---------- JWT Mock 설정 ----------
        setupJwtMocks();


    }

    // ================= HELPER =================
    private Complaint createComplaintForTest(ComplaintStatus status, Admin admin) {
        Complaint complaint = Complaint.builder()
                .member(member)
                .admin(admin)
                .type(ComplaintType.MAINTENANCE)
                .title("테스트 민원")
                .content("테스트 내용")
                .status(status)
                .build();

        return complaintRepository.save(complaint);
    }


    private void setupJwtMocks() {
        AdminDetails adminDetails1 = new AdminDetails(admin1);
        MemberDetails memberDetails = new MemberDetails(member);

        Mockito.when(jwtProvider.validateToken(Mockito.anyString())).thenReturn(true);
        Mockito.when(jwtProvider.getAuthentication(Mockito.anyString()))
                .thenAnswer(invocation -> {
                    String token = invocation.getArgument(0);
                    if (token.contains("admin")) {
                        return new UsernamePasswordAuthenticationToken(adminDetails1, null, adminDetails1.getAuthorities());
                    } else {
                        return new UsernamePasswordAuthenticationToken(memberDetails, null, memberDetails.getAuthorities());
                    }
                });
    }

    // ================= MEMBER =================
    @Test
    void createComplaint() throws Exception {
        mockMvc.perform(post("/api/complaint")
                        .header("Authorization", "Bearer member-token")
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
    void updateComplaint() throws Exception {
        mockMvc.perform(put("/api/complaint/{id}", complaintId)
                        .header("Authorization", "Bearer member-token")
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
    void deleteComplaint() throws Exception {
        mockMvc.perform(delete("/api/complaint/{id}", complaintId)
                        .header("Authorization", "Bearer member-token"))
                .andExpect(status().isOk());
    }

//    @Test
//    @Transactional
//    void createFeedback() throws Exception {
//        // 1️⃣ 테스트용 민원 생성 (COMPLETED)
//        Complaint complaint = createComplaintForTest(ComplaintStatus.COMPLETED, admin1);
//
//        // 2️⃣ SecurityContext에 Member 반영
//        SecurityContextHolder.getContext().setAuthentication(
//                new UsernamePasswordAuthenticationToken(
//                        new MemberDetails(member),
//                        null,
//                        new MemberDetails(member).getAuthorities()
//                )
//        );
//
//        // 3️⃣ DTO 준비
//        ComplaintFeedbackCreateRequest request = new ComplaintFeedbackCreateRequest(
//                "좋아요",
//                BigDecimal.valueOf(5)
//        );
//
//        // 4️⃣ MockMvc로 피드백 등록
//        mockMvc.perform(post("/api/complaint/{id}/feedbacks", complaint.getId())
//                        .header("Authorization", "Bearer member-token")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(request)))
//                .andExpect(status().isOk());
//
//        // 5️⃣ DB 저장 확인
//        ComplaintFeedback feedback = complaintFeedbackRepository
//                .findByComplaint_Id(complaint.getId())
//                .orElseThrow(() -> new AssertionError("피드백이 DB에 저장되지 않았습니다."));
//
//        Assertions.assertEquals("좋아요", feedback.getContent());
//        Assertions.assertEquals(0, BigDecimal.valueOf(5).compareTo(feedback.getRating()));
//        Assertions.assertEquals(member.getId(), feedback.getMember().getId());
//        Assertions.assertEquals(complaint.getId(), feedback.getComplaint().getId());
//    }



    // ================= ADMIN =================
    @Test
    void getComplaintsByApartment() throws Exception {
        mockMvc.perform(get("/api/complaint/apartment/{id}", apartmentId)
                        .header("Authorization", "Bearer admin-token"))
                .andExpect(status().isOk());
    }

//    @Test
//    void changeStatus() throws Exception {
//        mockMvc.perform(post("/api/complaint/{id}/status", complaintId)
//                        .header("Authorization", "Bearer admin-token")
//                        .param("status", ComplaintStatus.ASSIGNED.name()))
//                .andExpect(status().isOk());
//    }
//
//    @Test
//    void completeComplaint() throws Exception {
//        mockMvc.perform(post("/api/complaint/{id}/complete", complaintId)
//                        .header("Authorization", "Bearer admin-token"))
//                .andExpect(status().isOk());
//    }
//
//    @Test
//    void assignAdmin() throws Exception {
//        mockMvc.perform(post("/api/complaint/{id}/assign", complaintId)
//                        .header("Authorization", "Bearer admin-token")
//                        .param("targetAdminId", targetAdminId.toString()))
//                .andExpect(status().isOk());
//    }
//
//    @Test
//    void createAnswer() throws Exception {
//        mockMvc.perform(post("/api/complaint/{id}/answers", complaintId)
//                        .header("Authorization", "Bearer admin-token")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(
//                                new ComplaintAnswerCreateRequest("처리 완료")
//                        )))
//                .andExpect(status().isOk());
//    }
}
