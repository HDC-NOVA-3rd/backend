package com.backend.nova.auth.otp;

import com.backend.nova.admin.entity.OtpPurpose;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class StatelessOtpServiceImpl implements StatelessOtpService {

    /**
     * OTP 유효 시간 (초)
     * 5분 = 300초
     */
    private static final long OTP_WINDOW_SECONDS = 300;

    /**
     * OTP 길이
     */
    private static final int OTP_DIGITS = 6;

    /**
     * 재사용 방지용 (메모리)
     * key = hash(loginId + purpose + otp)
     */
    private final Set<String> usedOtps = ConcurrentHashMap.newKeySet();

    @Value("${otp.secret}")
    private String secret;

    private Mac mac;

    @PostConstruct
    void init() {
        try {
            mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(
                    secret.getBytes(StandardCharsets.UTF_8),
                    "HmacSHA256"
            ));
        } catch (Exception e) {
            throw new IllegalStateException("OTP 초기화 실패", e);
        }
    }

    /* ================= OTP 생성 ================= */

    @Override
    public String generate(String key, OtpPurpose purpose) {
        long window = currentWindow();

        String otp = generateOtp(key, purpose, window);

        log.debug("Generated OTP for key={}, purpose={}", key, purpose);
        return otp;
    }

    /* ================= OTP 검증 ================= */

    @Override
    public boolean verify(String key, OtpPurpose purpose, String inputOtp) {
        long nowWindow = currentWindow();

        // 현재 window ±1 허용
        for (long w = nowWindow - 1; w <= nowWindow + 1; w++) {
            String expected = generateOtp(key, purpose, w);

            if (expected.equals(inputOtp)) {

                // 재사용 방지
                String reuseKey = reuseKey(key, purpose, inputOtp);
                if (usedOtps.contains(reuseKey)) {
                    log.warn("OTP 재사용 시도 차단");
                    return false;
                }

                usedOtps.add(reuseKey);
                return true;
            }
        }

        return false;
    }

    /* ================= 내부 로직 ================= */

    private String generateOtp(String key, OtpPurpose purpose, long window) {
        try {
            String data = key + ":" + purpose.name() + ":" + window;
            byte[] hash;

            synchronized (mac) {
                hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            }

            int offset = hash[hash.length - 1] & 0x0F;

            int binary =
                    ((hash[offset] & 0x7f) << 24) |
                            ((hash[offset + 1] & 0xff) << 16) |
                            ((hash[offset + 2] & 0xff) << 8) |
                            (hash[offset + 3] & 0xff);

            int otp = binary % (int) Math.pow(10, OTP_DIGITS);

            return String.format("%0" + OTP_DIGITS + "d", otp);

        } catch (Exception e) {
            throw new IllegalStateException("OTP 생성 실패", e);
        }
    }

    private long currentWindow() {
        return Instant.now().getEpochSecond() / OTP_WINDOW_SECONDS;
    }

    private String reuseKey(String key, OtpPurpose purpose, String otp) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(
                    (key + purpose + otp).getBytes(StandardCharsets.UTF_8)
            );
            return Base64.getEncoder().encodeToString(hashed);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
