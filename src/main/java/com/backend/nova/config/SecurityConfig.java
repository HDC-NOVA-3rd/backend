package com.backend.nova.config;

import com.backend.nova.auth.admin.AdminAuthenticationProvider;
import com.backend.nova.auth.jwt.JwtAuthenticationEntryPoint;
import com.backend.nova.auth.jwt.JwtAuthenticationFilter;
import com.backend.nova.auth.jwt.JwtProvider;
import com.backend.nova.auth.member.MemberAuthenticationProvider;
import com.backend.nova.oauth2.handler.OAuthFailureHandler;
import com.backend.nova.oauth2.handler.OAuthSuccessHandler;
import com.backend.nova.oauth2.repository.OAuthRedirectCookieRepository;
import com.backend.nova.oauth2.service.CustomOAuth2UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtProvider jwtProvider;
    private final MemberAuthenticationProvider memberAuthenticationProvider;
    private final AdminAuthenticationProvider adminAuthenticationProvider;
    private final CustomOAuth2UserService customOAuth2UserService;
    private final OAuthSuccessHandler oAuthSuccessHandler;
    private final OAuthFailureHandler oAuthFailureHandler;
    private final OAuthRedirectCookieRepository oAuthRedirectCookieRepository;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    /**
     * AuthenticationManager Bean
     */
    @Bean
    public AuthenticationManager authenticationManager() throws Exception {
        return new ProviderManager(Arrays.asList(memberAuthenticationProvider, adminAuthenticationProvider));
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

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
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
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // 요청별 권한 설정
                .authorizeHttpRequests(auth -> auth

                        // 인증 없이 접근 가능
                        .requestMatchers("/api/admin/auth/**").permitAll()
                        .requestMatchers("/api/admin/account/**").permitAll()
                        .requestMatchers("/api/admin/complaint/**").permitAll()
                        .requestMatchers("/api/admin/management-fee/**").permitAll()
                        .requestMatchers("/api/admin/bill/**").permitAll()
                        .requestMatchers("/api/admin/notice/**").permitAll()
                        .requestMatchers("/api/notice/**").permitAll()
                        .requestMatchers("/api/admin/auth/refresh").permitAll()

                        .requestMatchers("/admin/rag/**").permitAll()
                        .requestMatchers("/api/safety/**").permitAll()
                        .requestMatchers("/api/apartment/**").permitAll()
                        .requestMatchers("/api/room/**").permitAll()



                        .requestMatchers("/api/resident/verify","/api/member/signup").permitAll()
                        //로그인 페이지 API -> 접근 허용
                        .requestMatchers("/api/member/refresh", "/api/member/login", "/api/member/findInfo", "/api/member/resetPW", "/api/member/oauth/exchange").permitAll()
                        //Swagger 페이지 API -> 접근 허용
                        .requestMatchers("/api", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        .requestMatchers("/api/voice/**").permitAll()
                        // 이미지 경로에 권한 x 처리
                        .requestMatchers("/images/**").permitAll()
                        .requestMatchers("/api/resident/verify","/api/member/signup").permitAll()

                        //로그인 페이지 API -> 접근 허용
                        .requestMatchers("/api/member/refresh", "/api/member/login", "/api/member/findInfo", "/api/member/resetPW", "/api/member/oauth/exchange").permitAll()

                        //Swagger 페이지 API -> 접근 허용
                        .requestMatchers("/api", "/swagger-ui/**", "/v3/api-docs/**","/api/chat/**").permitAll()

                        // 이미지 경로에 권한 x 처리
                        .requestMatchers("/images/**").permitAll()

                        // 관리자 생성 (슈퍼 관리자만)
                        .requestMatchers(HttpMethod.POST, "/api/admin/signup").hasRole("SUPER_ADMIN")

                        //.requestMatchers("/api/admin/password/**").authenticated()
                        //.requestMatchers("/api/admin/complaint/**").authenticated()
                        // 관리비 관련 API (인증 필요)
                        //.requestMatchers("/api/admin/management-fee/**").authenticated()
                        //.requestMatchers("/api/admin/bill/**").authenticated()

                        // 그 외 관리자 API
                        .anyRequest().hasRole("ADMIN")
                )

                // JWT 인증 필터 등록
                .addFilterBefore(
                        new JwtAuthenticationFilter(jwtProvider),
                        UsernamePasswordAuthenticationFilter.class
                )

                // JWT 인증 실패 시 401 처리
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(jwtAuthenticationEntryPoint)
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

                // MemberAuthenticationProvider 를 Provider에 등록
                // ID/PW 로그인 시, DB의 Member 테이블을 조회하여 검증하는 로직 담당
                .authenticationProvider(memberAuthenticationProvider)

                // CORS 설정
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // CSRF disable
                .csrf(AbstractHttpConfigurer::disable)

                // Form Login disable
                // 브라우저 기본 로그인 폼이나 Spring Security 기본 로그인 페이지를 쓰지 않음
                .formLogin(AbstractHttpConfigurer::disable)

                // OAuth2 Login 설정
                .oauth2Login(oauth2 -> oauth2
                        // 소셜 로그인 성공 후 사용자 정보를 가져오는 서비스 등록
                        .userInfoEndpoint(userInfoEndpointConfig -> userInfoEndpointConfig
                                .userService(customOAuth2UserService))
                        // 인증 요청 시 쿠키에 데이터를 저장하는 리포지토리 (redirect_uri 등을 저장)
                        .authorizationEndpoint(authorization -> authorization
                                .authorizationRequestRepository(oAuthRedirectCookieRepository))
                        // 로그인 성공 시 실행될 핸들러 (JWT를 발급 및 리다이렉트 처리)
                        .successHandler(oAuthSuccessHandler)
                        // 로그인 실패 시 실행 핸들러
                        .failureHandler(oAuthFailureHandler))

                // 세션 필터 설정 (STATELESS)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                // JWT 인증 실패 시 해당 EntryPoint가 실행됨 -> 401 리턴
                .exceptionHandling(exception-> exception
                        .authenticationEntryPoint(jwtAuthenticationEntryPoint))

                // 인가 처리
                .authorizeHttpRequests(authorize -> authorize
                        //회원 가입 페이지 API -> 접근 허용
                        .requestMatchers("/api/resident/verify","/api/member/signup").permitAll()
                        //로그인 페이지 API -> 접근 허용
                        .requestMatchers("/api/member/refresh", "/api/member/login", "/api/member/findInfo", "/api/member/resetPW", "/api/member/oauth/exchange").permitAll()
                        //Swagger 페이지 API -> 접근 허용
                        .requestMatchers("/api", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        .requestMatchers("/api/voice/**").permitAll()
                        //모니터링 툴 API -> 접근 허용
                        .requestMatchers("/actuator/**").permitAll()
                        .requestMatchers("/admin/rag/**").permitAll()
                        .requestMatchers("/ws/**").permitAll()
                        .requestMatchers("/api/safety/**").permitAll()
                        .requestMatchers("/api/apartment/**").permitAll()
                        .requestMatchers("/api/room/**").permitAll()
                        .requestMatchers("/api/mode/**").permitAll()
                        // 이미지 경로에 권한 x 처리
                        .requestMatchers("/images/**").permitAll()
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
