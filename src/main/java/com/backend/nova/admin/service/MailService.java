package com.backend.nova.admin.service;

import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MailService {

    private final JavaMailSender mailSender;

    public void sendOtpMail(String to, String otp) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("[아파트 관리자] 비밀번호 재설정 인증번호");
        message.setText("""
            요청하신 인증번호입니다.

            인증번호: %s

            5분 이내에 입력해주세요.
            """.formatted(otp));

        mailSender.send(message);
    }
}
