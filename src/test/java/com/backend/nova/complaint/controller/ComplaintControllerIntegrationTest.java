package com.backend.nova.complaint.controller;

import com.backend.nova.apartment.service.ApartmentWeatherService;
import com.backend.nova.auth.jwt.JwtProvider;
import com.backend.nova.complaint.dto.*;
import com.backend.nova.complaint.entity.ComplaintStatus;
import com.backend.nova.complaint.entity.ComplaintType;
import com.backend.nova.weather.service.OpenWeatherService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class ComplaintControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;


    // JwtProvider를 Mock 처리해서 실제 JWT 키 없이도 ApplicationContext 로드 가능
    @MockBean
    private JwtProvider jwtProvider;

    // 테스트에 필요 없는 빈들을 Mock 처리
    @MockBean
    private ApartmentWeatherService apartmentWeatherService;

    @MockBean
    private OpenWeatherService openWeatherService;

    // ======================== MEMBER ========================

    @Test
    @DisplayName("입주민 민원 등록 테스트")
    @WithMockUser(username = "member1", roles = {"MEMBER"})
    void testCreateComplaint() throws Exception {
        ComplaintCreateRequest request = new ComplaintCreateRequest(
                ComplaintType.MAINTENANCE,
                "배관 문제",
                "화장실 배관에서 물이 새요."
        );

        mockMvc.perform(MockMvcRequestBuilders.post("/api/complaint")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("입주민 민원 수정 테스트")
    @WithMockUser(username = "member1", roles = {"MEMBER"})
    void testUpdateComplaint() throws Exception {
        Long complaintId = 1L;
        ComplaintUpdateRequest request = new ComplaintUpdateRequest(
                ComplaintType.MAINTENANCE,
                "배관 문제 수정",
                "화장실 배관 물 새는 문제 해결 요청"
        );

        mockMvc.perform(MockMvcRequestBuilders.put("/api/complaint/{complaintId}", complaintId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("입주민 민원 삭제 테스트")
    @WithMockUser(username = "member1", roles = {"MEMBER"})
    void testDeleteComplaint() throws Exception {
        Long complaintId = 1L;

        mockMvc.perform(MockMvcRequestBuilders.delete("/api/complaint/{complaintId}", complaintId))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("회원별 민원 목록 조회 테스트")
    @WithMockUser(username = "member1", roles = {"MEMBER"})
    void testGetComplaintsByMember() throws Exception {
        Long memberId = 1L;

        mockMvc.perform(MockMvcRequestBuilders.get("/api/complaint/member/{memberId}", memberId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("민원 상세 조회 테스트")
    @WithMockUser(username = "member1", roles = {"MEMBER"})
    void testGetComplaint() throws Exception {
        Long complaintId = 1L;

        mockMvc.perform(MockMvcRequestBuilders.get("/api/complaint/{complaintId}", complaintId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(complaintId));
    }

    @Test
    @DisplayName("입주민 민원 피드백 등록 테스트")
    @WithMockUser(username = "member1", roles = {"MEMBER"})
    void testCreateFeedback() throws Exception {
        Long complaintId = 1L;
        ComplaintFeedbackCreateRequest request = new ComplaintFeedbackCreateRequest(
                "빠른 처리 감사합니다",
                BigDecimal.valueOf(5.01)
        );

        mockMvc.perform(MockMvcRequestBuilders.post("/api/complaint/{complaintId}/feedbacks", complaintId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    // ======================== ADMIN ========================

    @Test
    @DisplayName("관리자 아파트별 민원 목록 조회 테스트")
    @WithMockUser(username = "admin1", roles = {"ADMIN"})
    void testGetComplaintsByApartment() throws Exception {
        Long apartmentId = 1L;

        mockMvc.perform(MockMvcRequestBuilders.get("/api/complaint/apartment/{apartmentId}", apartmentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("관리자 민원 상태 변경 테스트")
    @WithMockUser(username = "admin1", roles = {"ADMIN"})
    void testChangeStatus() throws Exception {
        Long complaintId = 1L;

        mockMvc.perform(MockMvcRequestBuilders.post("/api/complaint/{complaintId}/status", complaintId)
                        .param("status", ComplaintStatus.ASSIGNED.name()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("관리자 민원 완료 테스트")
    @WithMockUser(username = "admin1", roles = {"ADMIN"})
    void testCompleteComplaint() throws Exception {
        Long complaintId = 1L;

        mockMvc.perform(MockMvcRequestBuilders.post("/api/complaint/{complaintId}/complete", complaintId))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("관리자 민원 담당자 배정 테스트")
    @WithMockUser(username = "admin1", roles = {"ADMIN"})
    void testAssignAdmin() throws Exception {
        Long complaintId = 1L;
        Long targetAdminId = 2L;

        mockMvc.perform(MockMvcRequestBuilders.post("/api/complaint/{complaintId}/assign", complaintId)
                        .param("targetAdminId", String.valueOf(targetAdminId)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("관리자 민원 답변 등록 테스트")
    @WithMockUser(username = "admin1", roles = {"ADMIN"})
    void testCreateAnswer() throws Exception {
        Long complaintId = 1L;
        ComplaintAnswerCreateRequest request = new ComplaintAnswerCreateRequest(
                "문제 확인 후 처리 완료"
        );

        mockMvc.perform(MockMvcRequestBuilders.post("/api/complaint/{complaintId}/answers", complaintId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }
}
