package com.backend.nova;

import com.backend.nova.admin.repository.AdminRepository;
import com.backend.nova.admin.service.MailService;
import com.backend.nova.auth.jwt.JwtProvider;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
class NovaApplicationTests {

    @MockitoBean
    private JwtProvider jwtProvider;

    @MockitoBean
    private AdminRepository adminRepository;

    @MockitoBean
    private MailService mailService;

	@Test
	void contextLoads() {
	}

}
