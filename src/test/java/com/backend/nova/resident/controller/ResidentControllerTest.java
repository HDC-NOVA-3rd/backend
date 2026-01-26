package com.backend.nova.resident.controller;

import com.backend.nova.resident.dto.ResidentRequestDto;
import com.backend.nova.resident.dto.ResidentVerifyResponseDto;
import com.backend.nova.resident.service.ResidentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ResidentController.class)
class ResidentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ResidentService residentService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("입주민 인증 성공 테스트")
    void verifyResident_Success() throws Exception {
        // given
        ResidentRequestDto requestDto = new ResidentRequestDto(1L, "홍길동", "010-1234-5678");
        ResidentVerifyResponseDto responseDto = new ResidentVerifyResponseDto(true,123L, "인증 성공");
        given(residentService.verifyResident(any(ResidentRequestDto.class))).willReturn(responseDto);

        // when & then
        mockMvc.perform(
                post("/api/resident/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isVerified").value(true))
                .andExpect(jsonPath("$.residentId").value(123))
                .andExpect(jsonPath("$.message").value("인증 성공"));
    }

    @Test
    @DisplayName("입주민 인증 실패 테스트")
    void verifyResident_Fail() throws Exception {
        // given
        ResidentRequestDto requestDto = new ResidentRequestDto(1L, "홍길동", "010-1234-5678");
        ResidentVerifyResponseDto responseDto = new ResidentVerifyResponseDto(false,null,"인증 실패");
        given(residentService.verifyResident(any(ResidentRequestDto.class))).willReturn(responseDto);

        // when & then
        mockMvc.perform(post("/api/resident/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isVerified").value(false))
                .andExpect(jsonPath("$.residentId").doesNotExist())
                .andExpect(jsonPath("$.message").value("인증 실패"));
    }
}