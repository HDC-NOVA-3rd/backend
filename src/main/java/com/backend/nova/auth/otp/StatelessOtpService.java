package com.backend.nova.auth.otp;

public interface StatelessOtpService {

    String generate(String key, OtpPurpose purpose);

    boolean verify(String key, OtpPurpose purpose, String otp);
}
