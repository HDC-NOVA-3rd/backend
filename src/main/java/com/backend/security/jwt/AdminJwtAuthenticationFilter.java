package com.backend.security.jwt;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

public class AdminJwtAuthenticationFilter extends OncePerRequestFilter {

    private final AdminJwtTokenProvider tokenProvider;

    public AdminJwtAuthenticationFilter(AdminJwtTokenProvider tokenProvider) {
        this.tokenProvider = tokenProvider;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String header = request.getHeader("Authorization");

        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);

            try {
                Claims claims = tokenProvider.parseClaims(token);

                if ("ADMIN".equals(claims.get("role"))) {
                    Long adminId = Long.valueOf(claims.getSubject());

                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(
                                    adminId,
                                    null,
                                    List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
                            );

                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }

            } catch (Exception e) {
                SecurityContextHolder.clearContext();
            }
        }

        filterChain.doFilter(request, response);
    }
}
