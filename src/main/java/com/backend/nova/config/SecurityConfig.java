package com.backend.nova.config;

import com.backend.nova.auth.jwt.JwtAuthenticationFilter;
import com.backend.nova.auth.jwt.JwtProvider;
import com.backend.nova.auth.member.MemberAuthenticationProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtProvider jwtProvider;
    private final MemberAuthenticationProvider memberAuthenticationProvider;

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    // 입주민 용 Security Filter Chain
    @Bean
    public SecurityFilterChain memberFilterChain(HttpSecurity http) throws Exception {
        http
                // 관리자 Chain에 들어갈 경로를 제외한 모든 요청 처리
                .securityMatcher("/**")
                // MemberAuthenticationProvider 를 시큐리티 로직에 사용하도록 설정
                .authenticationProvider(memberAuthenticationProvider)
                // CSRF 보안 필터 disable
                .csrf(AbstractHttpConfigurer::disable)
                // 기본 Form 기반 인증 필터들 disable
                .formLogin(AbstractHttpConfigurer::disable)
                // 세션 필터 설정 (STATELESS)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // 인가 처리
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/api/member/login", "/api/member/signup", "/api/resident/verify").permitAll()
                        .requestMatchers("/api", "/swagger-ui/**", "/v3/api-docs/**","/ai/chat/**").permitAll()
                        .requestMatchers("/api/safety/**").permitAll()
                        .anyRequest().authenticated()
                )
                // 커스텀 필터 설정 JwtFilter 선행 처리
                .addFilterBefore(new JwtAuthenticationFilter(jwtProvider), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}