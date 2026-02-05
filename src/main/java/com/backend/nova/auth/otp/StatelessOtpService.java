package com.backend.nova.auth.otp;

import com.backend.nova.admin.entity.OtpPurpose;

public interface StatelessOtpService {

    String generate(String key, OtpPurpose purpose);

    boolean verify(String key, OtpPurpose purpose, String otp);
}
