package com.backend.nova.admin.service;

import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class TotpUtil {

    private final Totp totp;

    public TotpUtil() {
        this.totp = new Totp(
                new DefaultCodeGenerator(),
                new SystemTimeProvider(),
                Duration.ofSeconds(30)
        );
    }

    public boolean verify(String secret, String code) {
        return totp.verify(code, secret);
    }

    public String generateSecret() {
        byte[] buffer = new byte[20];
        new SecureRandom().nextBytes(buffer);
        return new Base32().encodeToString(buffer);
    }
}
