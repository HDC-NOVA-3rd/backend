package com.backend.nova.config;

import com.backend.nova.auth.admin.AdminAuthenticationProvider;
import com.backend.nova.auth.jwt.JwtAuthenticationFilter;
import com.backend.nova.auth.jwt.JwtProvider;
import com.backend.nova.auth.member.MemberAuthenticationProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Collections;
import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtProvider jwtProvider;
    private final MemberAuthenticationProvider memberAuthenticationProvider;
    private final AdminAuthenticationProvider adminAuthenticationProvider;

    /**
     * AuthenticationManager Bean
     */
    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration authenticationConfiguration
    ) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    /**
     * CORS 설정
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("http://localhost:5173", "http://localhost:8081"));
        config.addAllowedMethod("*");
        config.addAllowedHeader("*");
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    /**
     * ===============================
     * 관리자용 Security Filter Chain
     * ===============================
     */
    @Bean
    @Order(1)
    public SecurityFilterChain adminFilterChain(HttpSecurity http) throws Exception {

        http
                // 관리자 API 경로만 처리
                .securityMatcher("/api/admin/**")

                // 관리자 AuthenticationProvider 사용
                .authenticationProvider(adminAuthenticationProvider)

                // CSRF 비활성화 (JWT 사용)
                .csrf(AbstractHttpConfigurer::disable)

                // CORS 설정
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // 세션 사용 안 함
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                // 요청별 권한 설정
                .authorizeHttpRequests(auth -> auth

                        // 인증 없이 접근 가능
                        .requestMatchers("/api/admin/login/**").permitAll()
                        .requestMatchers("/api/admin/password/**").permitAll()

                        // 관리자 생성 (슈퍼 관리자만)
                        .requestMatchers(HttpMethod.POST, "/api/admin")
                        .hasRole("SUPER_ADMIN")

                        // 그 외 관리자 API
                        .anyRequest().hasRole("ADMIN")
                )

                // JWT 인증 필터 등록
                .addFilterBefore(
                        new JwtAuthenticationFilter(jwtProvider),
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }

    /**
     * ===============================
     * 입주민 용 Security Filter Chain
     * ===============================
     */
    @Bean
    @Order(2)
    public SecurityFilterChain memberFilterChain(HttpSecurity http) throws Exception {

        http
                // 관리자 Chain에 들어갈 경로를 제외한 모든 요청 처리
                .securityMatcher("/**")

                // MemberAuthenticationProvider 를 시큐리티 로직에 사용하도록 설정
                .authenticationProvider(memberAuthenticationProvider)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                // CSRF 보안 필터 disable
                .csrf(AbstractHttpConfigurer::disable)

                // 기본 Form 기반 인증 필터들 disable
                .formLogin(AbstractHttpConfigurer::disable)

                // 세션 필터 설정 (STATELESS)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                // 인가 처리
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/api/member/login", "/api/member/signup", "/api/resident/verify", "/api/apartment/**").permitAll()
                        .requestMatchers("/api", "/swagger-ui/**", "/v3/api-docs/**","/ai/chat/**").permitAll()
                        .requestMatchers("/api/safety/**").permitAll()
                        .requestMatchers("/api/apartment/**").permitAll()
                        .anyRequest().authenticated()
                )

                // 커스텀 필터 설정 JwtFilter 선행 처리
                .addFilterBefore(
                        new JwtAuthenticationFilter(jwtProvider),
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }
}