package com.backend.nova.config;

import com.backend.nova.auth.admin.AdminAuthenticationProvider;
import com.backend.nova.auth.jwt.JwtProvider;
import com.backend.nova.auth.member.MemberAuthenticationProvider;
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
}
