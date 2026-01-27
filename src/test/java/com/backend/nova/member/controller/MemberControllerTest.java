package com.backend.nova.member.controller;

import com.backend.nova.auth.jwt.JwtProvider;
import com.backend.nova.config.SecurityConfig;
import com.backend.nova.member.dto.SignupRequest;
import com.backend.nova.member.entity.LoginType;
import com.backend.nova.member.service.MemberService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MemberController.class)
@Import(SecurityConfig.class)
class MemberControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MemberService memberService;

    @MockitoBean
    private JwtProvider jwtProvider;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("회원 가입 성공 테스트")
    void registerMember_Success() throws Exception {
        // given
        SignupRequest request = new SignupRequest(
                1L, "user123", "password", "user@example.com", "홍길동", "010-1234-5678",
                LocalDate.of(1990, 1, 1), LoginType.NORMAL, null);
        given(memberService.registerMember(any(SignupRequest.class))).willReturn(1L);

        // when & then
        mockMvc.perform(post("/api/member/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/member/1"));
    }
}
