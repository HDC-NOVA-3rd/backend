package com.backend.nova.admin.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MailService {

    // 실제 메일 발송용 JavaMailSender는 주석 처리
    // private final JavaMailSender mailSender;

    /**
     * 테스트용: OTP를 콘솔에 출력
     */
    public void sendOtpMail(String to, String otp) {
        System.out.printf("""
                [TEST] OTP 발송
                수신자: %s
                인증번호: %s
                5분 이내 입력
                ------------------------
                """, to, otp);

        // 실제 메일 보내기 코드 (운영 시 주석 해제)
        /*
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("[아파트 관리자] 비밀번호 재설정 인증번호");
        message.setText(String.format(
            "요청하신 인증번호입니다.\n\n인증번호: %s\n\n5분 이내에 입력해주세요.", otp
        ));
        mailSender.send(message);
        */
    }
}


//package com.backend.nova.admin.service;
//
//import lombok.RequiredArgsConstructor;
//import org.springframework.mail.SimpleMailMessage;
//import org.springframework.mail.javamail.JavaMailSender;
//import org.springframework.stereotype.Service;
//
//@Service
//@RequiredArgsConstructor
//public class MailService {
//
//    private final JavaMailSender mailSender;
//
//    public void sendOtpMail(String to, String otp) {
//        SimpleMailMessage message = new SimpleMailMessage();
//        message.setTo(to);
//        message.setSubject("[아파트 관리자] 비밀번호 재설정 인증번호");
//        message.setText("""
//            요청하신 인증번호입니다.
//
//            인증번호: %s
//
//            5분 이내에 입력해주세요.
//            """.formatted(otp));
//
//        mailSender.send(message);
//    }
//}
