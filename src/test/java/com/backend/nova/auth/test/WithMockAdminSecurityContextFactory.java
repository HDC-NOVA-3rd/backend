package com.backend.nova.auth.test;

import com.backend.nova.admin.entity.Admin;
import com.backend.nova.auth.admin.AdminDetails;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithSecurityContextFactory;

import java.util.List;

public class WithMockAdminSecurityContextFactory
        implements WithSecurityContextFactory<WithMockAdmin> {

    @Override
    public SecurityContext createSecurityContext(WithMockAdmin annotation) {
        Admin admin = Admin.builder().id(1L).build();
        AdminDetails adminDetails = new AdminDetails(admin);

        Authentication auth = new UsernamePasswordAuthenticationToken(
                adminDetails, null,
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(auth);
        return context;
    }
}