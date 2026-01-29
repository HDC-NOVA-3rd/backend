package com.backend.nova.config;

import com.backend.nova.auth.admin.AdminAuthenticationProvider;
import com.backend.nova.auth.jwt.JwtProvider;
import com.backend.nova.auth.member.MemberAuthenticationProvider;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

import static org.mockito.Mockito.mock;

@TestConfiguration
public class TestSecurityConfig {

    /* ===== Security 완전 오픈 ===== */
    @Bean
    public SecurityFilterChain testSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().permitAll()
                );
        return http.build();
    }

    /* ===== JWT / Provider Mock ===== */
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

}
