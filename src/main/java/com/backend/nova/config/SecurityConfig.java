package com.backend.nova.config;

import com.backend.nova.auth.jwt.AdminJwtAuthenticationFilter;
import com.backend.nova.auth.jwt.AdminJwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final AdminJwtTokenProvider tokenProvider;

    /**
     * 관리자 JWT 인증 필터
     */
    @Bean
    public AdminJwtAuthenticationFilter adminJwtAuthenticationFilter() {
        return new AdminJwtAuthenticationFilter(tokenProvider);
    }

    /**
     * Spring Security Filter Chain 설정
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http
                // CSRF 비활성화 (JWT 사용)
                .csrf(csrf -> csrf.disable())

                // CORS 설정
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // JWT 인증 필터 등록
                .addFilterBefore(
                        adminJwtAuthenticationFilter(),
                        UsernamePasswordAuthenticationFilter.class
                )

                // 요청별 권한 설정
                .authorizeHttpRequests(auth -> auth

                        // 🔓 인증 없이 접근 가능 (관리자 로그인 / 비밀번호 관련)
                        .requestMatchers("/api/admin/login/**").permitAll()
                        .requestMatchers("/api/admin/password/**").permitAll()

                        // 🔐 관리자 생성 (슈퍼관리자만 가능)
                        // POST /api/admin
                        .requestMatchers(HttpMethod.POST, "/api/admin")
                        .hasRole("SUPER_ADMIN")

                        // 🔐 그 외 관리자 API (ADMIN 이상)
                        .requestMatchers("/api/admin/**")
                        .hasRole("ADMIN")

                        // 🔓 Preflight 요청 허용
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // 🔐 나머지는 인증 필요
                        .anyRequest().authenticated()
                );

        return http.build();
    }

    /**
     * AuthenticationManager Bean
     */
    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config
    ) throws Exception {
        return config.getAuthenticationManager();
    }

    /**
     * CORS 설정
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.addAllowedOrigin("http://localhost:5173");
        config.addAllowedMethod("*");
        config.addAllowedHeader("*");
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
