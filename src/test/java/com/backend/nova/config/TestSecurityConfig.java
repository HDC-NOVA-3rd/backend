package com.backend.nova.config;

import com.backend.nova.auth.admin.AdminAuthenticationProvider;
import com.backend.nova.auth.jwt.JwtProvider;
import com.backend.nova.auth.member.MemberAuthenticationProvider;
import com.backend.nova.member.service.MemberService;
import com.backend.nova.resident.service.ResidentService;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;

import static org.mockito.Mockito.mock;

@TestConfiguration
public class TestSecurityConfig {

    @Bean
    public JwtProvider jwtProvider() {
        return mock(JwtProvider.class);
    }

    @Bean
    public MemberAuthenticationProvider memberAuthenticationProvider() {
        return mock(MemberAuthenticationProvider.class);
    }

    @Bean
    public AdminAuthenticationProvider adminAuthenticationProvider() {
        return mock(AdminAuthenticationProvider.class);
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration configuration
    ) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public MemberService memberService() {
        return mock(MemberService.class);
    }

    @Bean
    public ResidentService residentService() {
        return mock(ResidentService.class);
    }
}
