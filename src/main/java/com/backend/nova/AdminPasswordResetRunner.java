package com.backend.nova;

import com.backend.nova.admin.entity.Admin;
import com.backend.nova.admin.repository.AdminRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AdminPasswordResetRunner implements CommandLineRunner {

    private final AdminRepository adminRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        Admin admin = adminRepository.findByLoginId("admin")
                .orElseThrow();

        admin.setPasswordHash(passwordEncoder.encode("1234"));
        adminRepository.save(admin);

        System.out.println("🔐 admin 비밀번호 1234로 리셋 완료");
    }
}
