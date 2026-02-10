package com.backend.nova;

import com.backend.nova.auth.admin.AdminAuthenticationProvider;
import com.backend.nova.auth.admin.AdminDetailsService;
import com.backend.nova.auth.jwt.JwtAuthenticationEntryPoint;
import com.backend.nova.auth.jwt.JwtProvider;
import com.backend.nova.auth.member.MemberAuthenticationProvider;
import com.backend.nova.auth.member.MemberDetailsService;
import com.backend.nova.config.SecurityConfig;
import com.backend.nova.oauth2.handler.OAuthFailureHandler;
import com.backend.nova.oauth2.handler.OAuthSuccessHandler;
import com.backend.nova.oauth2.repository.OAuthRedirectCookieRepository;
import com.backend.nova.oauth2.service.CustomOAuth2UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@Import(SecurityConfig.class) // 공통 설정 Import
public abstract class ControllerTestSupport {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    // --- 공통 Mock Bean 정의 (모든 컨트롤러 테스트에서 필요한 것들) ---

    @MockitoBean
    protected JwtProvider jwtProvider;

    @MockitoBean
    protected MemberAuthenticationProvider memberAuthenticationProvider;

    @MockitoBean
    protected AdminAuthenticationProvider adminAuthenticationProvider;

    @MockitoBean
    protected CustomOAuth2UserService customOAuth2UserService;

    @MockitoBean
    protected OAuthSuccessHandler oAuthSuccessHandler;

    @MockitoBean
    protected OAuthFailureHandler oAuthFailureHandler;

    @MockitoBean
    protected OAuthRedirectCookieRepository oAuthRedirectCookieRepository;

    @MockitoBean
    protected ClientRegistrationRepository clientRegistrationRepository;

    @MockitoBean
    protected JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @MockitoBean
    protected AdminDetailsService adminDetailsService;

    @MockitoBean
    protected MemberDetailsService memberDetailsService;
}