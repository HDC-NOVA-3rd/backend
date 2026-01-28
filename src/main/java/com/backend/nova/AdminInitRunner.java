package com.backend.nova;

import com.backend.nova.admin.entity.Admin;
import com.backend.nova.admin.entity.AdminStatus;
import com.backend.nova.admin.repository.AdminRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AdminInitRunner implements CommandLineRunner {

    private final AdminRepository adminRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (adminRepository.findByLoginId("admin").isPresent()) {
            return;
        }

        Admin admin = Admin.builder()
                .loginId("admin")
                .passwordHash(passwordEncoder.encode("1234"))
                .name("테스트 관리자")
                .email("admin@test.com")
                .status(AdminStatus.ACTIVE)
                .build();

        adminRepository.save(admin);

        System.out.println("✅ 초기 관리자 계정 생성 완료");
    }

}

