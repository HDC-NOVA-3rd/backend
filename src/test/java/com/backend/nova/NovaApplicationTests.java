package com.backend.nova;


import com.backend.nova.auth.otp.StatelessOtpService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
class NovaApplicationTests {

    @MockitoBean
    private JavaMailSender javaMailSender;

    @MockitoBean
    private StatelessOtpService statelessOtpService;

	@Test
	void contextLoads() {
	}

}
