package com.backend.nova.resident.controller;

import com.backend.nova.ControllerTestSupport;
import com.backend.nova.auth.admin.AdminDetails;
import com.backend.nova.resident.dto.*;
import com.backend.nova.resident.service.ResidentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ResidentController.class)
class ResidentControllerTest extends ControllerTestSupport {

    @MockitoBean
    private ResidentService residentService;

    @BeforeEach
    void setUp() {
        AdminDetails adminDetails = mock(AdminDetails.class);
        given(adminDetails.getApartmentId()).willReturn(1L);

        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(adminDetails, null, List.of());

        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    @DisplayName("입주민 상세 조회 테스트")
    void getResident_Success() throws Exception {
        ResidentResponse response =
                new ResidentResponse(1L, "아파트", "101", "101", 3L, "010-1234-5678","");

        given(residentService.getResident(anyLong(), anyLong()))
                .willReturn(response);

        mockMvc.perform(get("/api/resident/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.residentId").value(1));
    }

    @Test
    @DisplayName("아파트별 입주민 목록 조회 테스트")
    void getAllResidents_Success() throws Exception {
        ResidentResponse response =
                new ResidentResponse(1L, "아파트", "101", "101", 3L, "010-1234-5678","");

        PageImpl<ResidentResponse> page =
                new PageImpl<>(List.of(response), PageRequest.of(0, 10), 1);

        given(residentService.getAllResidents(anyLong(), any(), any(), any()))
                .willReturn(page);

        mockMvc.perform(get("/api/resident/apartment")
                        .param("dongId", "1")
                        .param("searchTerm", "이름"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].residentId").value(1));
    }

    @Test
    @DisplayName("입주민 등록 테스트")
    void createResident_Success() throws Exception {
        ResidentSaveRequest request =
                new ResidentSaveRequest(1L, "이름", "010-1234-5678", "101", "101");

        given(residentService.createResident(any(ResidentSaveRequest.class), anyLong()))
                .willReturn(1L);

        mockMvc.perform(post("/api/resident")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/resident/1"));
    }

    @Test
    @DisplayName("입주민 정보 수정 테스트")
    void updateResident_Success() throws Exception {
        ResidentSaveRequest request =
                new ResidentSaveRequest(1L, "수정된이름", "010-1234-5678", "101", "102");

        mockMvc.perform(put("/api/resident/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("입주민 삭제 테스트")
    void deleteResident_Success() throws Exception {
        mockMvc.perform(delete("/api/resident/1"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("세대 입주민 리스트 삭제 테스트")
    void deleteAllResidents_Success() throws Exception {
        mockMvc.perform(delete("/api/resident/ho/1"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("입주민 인증 테스트")
    void verifyResident_Success() throws Exception {
        ResidentRequest request =
                new ResidentRequest(1L, "홍길동", "010-1234-5678");

        ResidentVerifyResponse response = ResidentVerifyResponse.builder()
                .isVerified(true)
                .residentId(123L)
                .name("홍길동")
                .status(SignupStatus.AVAILABLE)
                .build();

        given(residentService.verifyResident(any(ResidentRequest.class)))
                .willReturn(response);

        mockMvc.perform(post("/api/resident/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isVerified").value(true))
                .andExpect(jsonPath("$.residentId").value(123));
    }
}