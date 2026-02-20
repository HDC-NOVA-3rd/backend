package com.backend.nova.resident.controller;

import com.backend.nova.ControllerTestSupport;
import com.backend.nova.resident.dto.ResidentRequest;
import com.backend.nova.resident.dto.ResidentResponse;
import com.backend.nova.resident.dto.ResidentVerifyResponse;
import com.backend.nova.resident.service.ResidentService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import java.util.List;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ResidentController.class)
class ResidentControllerTest extends ControllerTestSupport {
    @MockitoBean
    private ResidentService residentService;

    @Test
    @DisplayName("입주민 상세 조회 테스트")
    @WithMockUser
    void getResident_Success() throws Exception {
        // given
        ResidentResponse response = new ResidentResponse(1L, "Apartment", "101", "101", "Name", "010-1234-5678");
        given(residentService.getResident(anyLong(),anyLong())).willReturn(response);

        // when & then
        mockMvc.perform(get("/api/resident/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.residentId").value(1L));
    }

    @Test
    @DisplayName("아파트별 입주민 목록 조회 테스트")
    @WithMockUser
    void getAllResidents_Success() throws Exception {
        // given
        List<ResidentResponse> response = List.of(new ResidentResponse(1L, "Apartment", "101", "101", "Name", "010-1234-5678"));
        given(residentService.getAllResidents(anyLong())).willReturn(response);

        // when & then
        mockMvc.perform(get("/api/resident/apartment/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size()").value(1));
    }

    @Test
    @DisplayName("입주민 등록 테스트")
    @WithMockUser
    void createResident_Success() throws Exception {
        // given
        ResidentRequest request = new ResidentRequest(1L, "Name", "010-1234-5678");
        given(residentService.createResident(any(), any())).willReturn(1L);

        // when & then
        mockMvc.perform(post("/api/resident")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/resident/1"));
    }

    @Test
    @DisplayName("입주민 정보 수정 테스트")
    @WithMockUser
    void updateResident_Success() throws Exception {
        // given
        ResidentRequest request = new ResidentRequest(1L, "Updated Name", "010-1234-5678");

        // when & then
        mockMvc.perform(put("/api/resident/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("입주민 삭제 테스트")
    @WithMockUser
    void deleteResident_Success() throws Exception {
        // when & then
        mockMvc.perform(delete("/api/resident/1"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("세대 입주민 리스트 삭제 테스트")
    @WithMockUser
    void deleteAllResidents_Success() throws Exception {
        // when & then
        mockMvc.perform(delete("/api/resident/ho/1"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("입주민 인증 성공 테스트")
    @WithMockUser
    void verifyResident_Success() throws Exception {
        // given
        ResidentRequest request = new ResidentRequest(1L, "홍길동", "010-1234-5678");
        ResidentVerifyResponse response = ResidentVerifyResponse.builder()
                .isVerified(true)
                .residentId(123L)
                .name("홍길동")
                .build();

        given(residentService.verifyResident(any())).willReturn(response);

        // when & then
        mockMvc.perform(post("/api/resident/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isVerified").value(true))
                .andExpect(jsonPath("$.residentId").value(123));
    }

    @Test
    @DisplayName("입주민 인증 실패 테스트")
    @WithMockUser
    void verifyResident_Fail() throws Exception {
        // given
        ResidentRequest request = new ResidentRequest(1L, "홍길동", "010-1234-5678");
        ResidentVerifyResponse response = ResidentVerifyResponse.builder()
                .isVerified(false)
                .build();

        given(residentService.verifyResident(any())).willReturn(response);

        // when & then
        mockMvc.perform(post("/api/resident/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isVerified").value(false))
                .andExpect(jsonPath("$.residentId").doesNotExist());
    }
}