package com.backend.nova.auth.admin;

import com.backend.nova.admin.entity.Admin;
import com.backend.nova.admin.repository.AdminRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminDetailsService implements UserDetailsService {

    private final AdminRepository adminRepository;

    /* ================= loginId 기반 (Spring Security 기본) ================= */

    @Override
    public UserDetails loadUserByUsername(String loginId)
            throws UsernameNotFoundException {

        Admin admin = adminRepository.findByLoginId(loginId)
                .orElseThrow(() ->
                        new UsernameNotFoundException("Admin not found: " + loginId));

        return new AdminDetails(admin);
    }

    /* ================= ID 기반 (JWT 전용) ================= */

    public UserDetails loadAdminById(Long adminId) {
        Admin admin = adminRepository.findById(adminId)
                .orElseThrow(() ->
                        new UsernameNotFoundException("Admin not found id=" + adminId));

        return new AdminDetails(admin);
    }
}
