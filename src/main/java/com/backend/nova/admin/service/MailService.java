package com.backend.nova.admin.service;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MailService {

    private final JavaMailSender mailSender;
    private static final Logger logger = LoggerFactory.getLogger(MailService.class);

    /**
     * OTP 메일 발송
     *
     * @param to  수신자 이메일
     * @param otp 발급된 OTP
     */
    public void sendOtpMail(String to, String otp) {

        // 콘솔 및 로그 출력 (스웨거 테스트용)
        logger.info("[TEST] OTP 발송 - 수신자: {}, 인증번호: {}", to, otp);
        System.out.printf("""
                [TEST] OTP 발송
                수신자: %s
                인증번호: %s
                5분 이내 입력
                ------------------------
                """, to, otp);

        // 실제 메일 발송
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("[아파트 관리자] 인증번호 안내");
        message.setText(String.format(
                "안녕하세요.\n\n요청하신 인증번호입니다.\n\n인증번호: %s\n\n5분 이내에 입력해주세요.",
                otp
        ));
        mailSender.send(message);
    }
}
