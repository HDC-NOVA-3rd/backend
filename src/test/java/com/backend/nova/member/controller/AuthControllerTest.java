package com.backend.nova.member.controller;

import com.backend.nova.ControllerTestSupport;
import com.backend.nova.member.dto.*;
import com.backend.nova.member.entity.LoginType;
import com.backend.nova.member.service.MemberService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
public class AuthControllerTest extends ControllerTestSupport {
    @MockitoBean
    private MemberService memberService;

    @Test
    @DisplayName("로그인 성공 테스트")
    void login_Success() throws Exception {
        // given
        LoginRequest loginRequest = new LoginRequest("user123", "password");

        TokenResponse tokenResponse = TokenResponse.builder()
                .accessToken("access-token")
                .refreshToken("refresh-token")
                .build();

        given(memberService.login(any()))
                .willReturn(tokenResponse);

        // when & then
        mockMvc.perform(post("/api/member/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access-token"))
                .andExpect(jsonPath("$.refreshToken").value("refresh-token"));
    }

    @Test
    @DisplayName("회원 가입 성공 테스트")
    void registerMember_Success() throws Exception {
        // given
        SignupRequest request = new SignupRequest(
                1L,
                "user123",
                "password",
                "user@example.com",
                "홍길동",
                "010-1234-5678",
                LocalDate.of(1990, 1, 1),
                LoginType.NORMAL,
                null
        );

        TokenResponse tokenResponse = TokenResponse.builder()
                .accessToken("access-token")
                .refreshToken("refresh-token")
                .build();

        given(memberService.registerMember(any()))
                .willReturn(tokenResponse);

        // when & then
        mockMvc.perform(post("/api/member/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access-token"));
    }

    @Test
    @DisplayName("토큰 재발급 테스트")
    void refresh_Success() throws Exception {
        // given
        RefreshTokenRequest request = new RefreshTokenRequest("refresh-token");
        TokenResponse response = TokenResponse.builder()
                .accessToken("new-access-token")
                .refreshToken("new-refresh-token")
                .build();

        given(memberService.refresh(any()))
                .willReturn(response);

        // when & then
        mockMvc.perform(post("/api/member/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("new-access-token"));
    }

    @Test
    @DisplayName("아이디 찾기 테스트")
    void findMemberId_Success() throws Exception {
        // given
        FindIdRequest request = new FindIdRequest("홍길동", "010-1234-5678");
        FindIdResponse response = FindIdResponse.builder()
                .loginType(LoginType.NORMAL)
                .message("아이디는 test*** 입니다.")
                .build();

        given(memberService.findMemberId(any()))
                .willReturn(response);

        // when & then
        mockMvc.perform(post("/api/member/findInfo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.loginType").value("NORMAL"));
    }

    @Test
    @DisplayName("비밀번호 재설정 요청 테스트")
    void resetPassword_Success() throws Exception {
        // given
        ResetPWRequest request = new ResetPWRequest("user123", "홍길동", "010-1234-5678");
        ResetPWResponse response = ResetPWResponse.builder()
                .tempPassword("temp123")
                .message("임시 비밀번호가 발급되었습니다.")
                .build();

        given(memberService.resetPassword(any()))
                .willReturn(response);

        // when & then
        mockMvc.perform(post("/api/member/resetPW")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tempPassword").value("temp123"));
    }

}
