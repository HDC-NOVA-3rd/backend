package com.backend.nova.member.controller;

import com.backend.nova.ControllerTestSupport;
import com.backend.nova.member.dto.*;
import com.backend.nova.member.entity.WithMockMember;
import com.backend.nova.member.service.MemberService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(MemberController.class)
class MemberControllerTest extends ControllerTestSupport {
    @MockitoBean
    private MemberService memberService;

    @Test
    @DisplayName("내 정보 조회 테스트")
    @WithMockMember(memberId = 1L)
    void getMyInfo_Success() throws Exception {
        // given
        MemberInfoResponse response = MemberInfoResponse.builder()
                .name("홍길동")
                .email("test@example.com")
                .build();

        given(memberService.getMemberInfo(anyString()))
                .willReturn(response);

        // when & then
        mockMvc.perform(get("/api/member/profile"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("홍길동"))
                .andExpect(jsonPath("$.email").value("test@example.com"));
    }

    @Test
    @DisplayName("내 아파트 정보 조회 테스트")
    @WithMockMember(memberId = 1L)
    void getMyApartmentInfo_Success() throws Exception {
        // given
        MemberApartmentResponse response = MemberApartmentResponse.builder()
                .apartmentName("행복아파트")
                .dongNo("101")
                .hoNo("101")
                .build();

        given(memberService.getMemberApartmentInfo(anyString()))
                .willReturn(response);

        // when & then
        mockMvc.perform(get("/api/member/apartment"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.apartmentName").value("행복아파트"))
                .andExpect(jsonPath("$.dongNo").value("101"))
                .andExpect(jsonPath("$.hoNo").value("101"));
    }

    @Test
    @DisplayName("비밀번호 변경 테스트")
    @WithMockMember(memberId = 1L)
    void changePassword_Success() throws Exception {
        // given
        ChangePWRequest request = new ChangePWRequest("oldPass", "newPass");

        // when & then
        mockMvc.perform(put("/api/member/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }
}