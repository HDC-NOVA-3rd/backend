//package com.backend.nova.auth.test;
//
//import com.backend.nova.admin.entity.Admin;
//import com.backend.nova.admin.entity.AdminRole;
//import com.backend.nova.admin.entity.AdminStatus;
//import com.backend.nova.apartment.entity.Apartment;
//import com.backend.nova.auth.admin.AdminDetails;
//import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
//import org.springframework.security.core.Authentication;
//import org.springframework.security.core.authority.SimpleGrantedAuthority;
//import org.springframework.security.core.context.SecurityContext;
//import org.springframework.security.core.context.SecurityContextHolder;
//import org.springframework.security.test.context.support.WithSecurityContextFactory;
//
//import java.util.List;
//
//public class WithMockAdminSecurityContextFactory
//        implements WithSecurityContextFactory<WithMockAdmin> {
//
//    @Override
//    public SecurityContext createSecurityContext(WithMockAdmin annotation) {
//
//        Apartment apartment = Apartment.builder()
//                .id(1L) // 테스트 DB apartmentId와 맞추면 더 안전
//                .build();
//
//        Admin admin = Admin.builder()
//                .id(annotation.adminId()) // ⭐ DB ID와 일치
//                .apartment(apartment)
//                .role(AdminRole.MANAGER)
//                .status(AdminStatus.ACTIVE)
//                .build();
//
//        AdminDetails details = new AdminDetails(admin);
//
//        Authentication auth = new UsernamePasswordAuthenticationToken(
//                details, null, details.getAuthorities()
//        );
//
//        SecurityContext context = SecurityContextHolder.createEmptyContext();
//        context.setAuthentication(auth);
//        return context;
//    }
//
//
//}

package com.backend.nova.auth.test;

import com.backend.nova.admin.entity.Admin;
import com.backend.nova.admin.entity.AdminRole;
import com.backend.nova.admin.entity.AdminStatus;
import com.backend.nova.apartment.entity.Apartment;
import com.backend.nova.auth.admin.AdminDetails;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithSecurityContextFactory;

public class WithMockAdminSecurityContextFactory
        implements WithSecurityContextFactory<WithMockAdmin> {

    @Override
    public SecurityContext createSecurityContext(WithMockAdmin annotation) {

        Apartment apartment = Apartment.builder()
                .id(1L)
                .build();

        Admin admin = Admin.builder()
                .id(1L)
                .apartment(apartment)
                .role(AdminRole.MANAGER)
                .status(AdminStatus.ACTIVE)
                .build();

        AdminDetails details = new AdminDetails(admin);

        Authentication auth = new UsernamePasswordAuthenticationToken(
                details, null, details.getAuthorities()
        );

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(auth);
        return context;
    }
}
