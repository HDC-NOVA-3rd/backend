package com.backend.nova.auth.admin;

//package com.fiveguys.smartapartment.backend.admin.security;

import com.backend.nova.admin.entity.Admin;
import com.backend.nova.admin.repository.AdminRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AdminDetailsService implements UserDetailsService {

    private final AdminRepository adminRepository;

    @Override
    public UserDetails loadUserByUsername(String loginId) throws UsernameNotFoundException {
        Admin admin = adminRepository.findByLoginId(loginId)
                .orElseThrow(() -> new UsernameNotFoundException("Admin not found: " + loginId));
        return new AdminDetails(admin);
    }
}
