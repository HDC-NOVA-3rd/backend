package com.backend.nova.config;

import com.backend.nova.auth.jwt.AdminJwtAuthenticationFilter;
import com.backend.nova.auth.jwt.AdminJwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import com.backend.nova.auth.jwt.JwtAuthenticationFilter;
import com.backend.nova.auth.jwt.JwtProvider;
import com.backend.nova.auth.member.MemberAuthenticationProvider;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;

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
                        .requestMatchers("/api", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        .anyRequest().authenticated()
                )
                // 커스텀 필터 설정 JwtFilter 선행 처리
                .addFilterBefore(new JwtAuthenticationFilter(jwtProvider), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
