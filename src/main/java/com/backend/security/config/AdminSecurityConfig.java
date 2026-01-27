package com.backend.security.config;

import com.backend.security.jwt.AdminJwtAuthenticationFilter;
import com.backend.security.jwt.AdminJwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@RequiredArgsConstructor
public class AdminSecurityConfig {

    private final AdminJwtTokenProvider adminJwtTokenProvider;

    @Bean
    public SecurityFilterChain adminSecurityFilterChain(HttpSecurity http) throws Exception {

        http
                .securityMatcher("/api/admin/**")
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm ->
                        sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/api/admin/login",
                                "/api/admin/password/**"
                        ).permitAll()
                        .anyRequest().hasRole("ADMIN")
                )
                .addFilterBefore(
                        new AdminJwtAuthenticationFilter(adminJwtTokenProvider),
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }
}

