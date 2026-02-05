package com.backend.nova.admin.service;

import org.apache.commons.codec.binary.Base32;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
public class TotpService {

    private final Totp totp;

    public TotpService() {
        this.totp = new Totp(
                new DefaultCodeGenerator(),
                new SystemTimeProvider(),
                Duration.ofSeconds(30)
        );
    }

    public String generateSecret() {
        byte[] buffer = new byte[20];
        new SecureRandom().nextBytes(buffer);
        return new Base32().encodeToString(buffer);
    }

    public boolean verify(String secret, String code) {
        return totp.verify(code, secret);
    }

    public String generateOtpAuthUrl(String loginId, String secret) {
        return String.format(
                "otpauth://totp/NOVA_ADMIN:%s?secret=%s&issuer=NOVA",
                loginId, secret
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
