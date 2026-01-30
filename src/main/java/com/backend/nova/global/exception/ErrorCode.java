package com.backend.nova.global.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {

    // ================= Admin =================
    ADMIN_LOGIN_ID_DUPLICATED(HttpStatus.CONFLICT, "이미 존재하는 관리자 ID입니다."),
    ADMIN_EMAIL_DUPLICATED(HttpStatus.CONFLICT, "이미 존재하는 관리자 이메일입니다."),

    // 로그인에서 사용하지 말 것 (조회/관리 API용)
    ADMIN_NOT_FOUND(HttpStatus.NOT_FOUND, "관리자를 찾을 수 없습니다."),

    // 로그인 실패 전용 (존재 X / 비번 오류 공통)
    ADMIN_LOGIN_FAILED(HttpStatus.UNAUTHORIZED, "아이디 또는 비밀번호가 올바르지 않습니다."),

    // 비밀번호 변경 전용
    INVALID_PASSWORD(HttpStatus.BAD_REQUEST, "현재 비밀번호가 올바르지 않습니다."),

    ADMIN_INACTIVE(HttpStatus.FORBIDDEN, "계정이 비활성 상태입니다."),
    ADMIN_LOCKED(HttpStatus.FORBIDDEN, "계정이 잠금 상태입니다."),


    // ================= Apartment =================
    APARTMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "아파트를 찾을 수 없습니다."),


    // ================= OTP =================
    OTP_NOT_FOUND(HttpStatus.NOT_FOUND, "OTP를 찾을 수 없습니다."),
    OTP_INVALID(HttpStatus.BAD_REQUEST, "OTP가 올바르지 않습니다."),
    OTP_EXPIRED(HttpStatus.BAD_REQUEST, "OTP가 만료되었습니다."),
    OTP_MAX_ATTEMPTS(HttpStatus.BAD_REQUEST, "OTP 시도 횟수를 초과했습니다."),
    OTP_NOT_VERIFIED(HttpStatus.BAD_REQUEST, "OTP 검증이 필요합니다."),


    // ================= Auth =================
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "인증이 필요합니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "권한이 없습니다."),


    // ================= Common =================
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "요청값이 올바르지 않습니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다.");

    private final HttpStatus status;
    private final String message;

    ErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }
}
