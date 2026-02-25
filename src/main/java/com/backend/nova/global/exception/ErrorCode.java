package com.backend.nova.global.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {

    // ================= Admin =================
    ADMIN_LOGIN_ID_DUPLICATED(HttpStatus.CONFLICT, "이미 존재하는 관리자 ID입니다."),
    ADMIN_EMAIL_DUPLICATED(HttpStatus.CONFLICT, "이미 존재하는 관리자 이메일입니다."),
    ADMIN_OTP_REQUIRED(HttpStatus.CONFLICT, "관리자는 로그인시 OTP가 필요합니다."),
    ADMIN_NOT_FOUND(HttpStatus.NOT_FOUND, "입력하신 정보와 일치하는 계정을 찾을 수 없습니다."),
    ADMIN_LOGIN_FAILED(HttpStatus.UNAUTHORIZED, "아이디 또는 비밀번호가 올바르지 않습니다."),
    INVALID_PASSWORD(HttpStatus.BAD_REQUEST, "현재 비밀번호가 올바르지 않습니다."),
    PASSWORD_NOT_MATCH(HttpStatus.BAD_REQUEST, "현재 비밀번호가 올바르지 않습니다."),
    PASSWORD_CONFIRM_NOT_MATCH(HttpStatus.BAD_REQUEST, "비밀번호 확인이 일치하지 않습니다."),
    ADMIN_INACTIVE(HttpStatus.FORBIDDEN, "계정이 비활성 상태입니다."),
    ADMIN_LOCKED(HttpStatus.FORBIDDEN, "계정이 잠금 상태입니다."),

    // ================= Apartment =================
    APARTMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "아파트를 찾을 수 없습니다."),
    HO_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 호 정보를 찾을 수 없습니다."),
    FACILITY_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 시설을 찾을 수 없습니다."),

    // ================= Notice =================
    NOTICE_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 공지를 찾을 수 없습니다."),

    // ================= OTP =================
    OTP_NOT_FOUND(HttpStatus.NOT_FOUND, "OTP를 찾을 수 없습니다."),
    OTP_INVALID(HttpStatus.BAD_REQUEST, "OTP가 올바르지 않습니다."),
    OTP_EXPIRED(HttpStatus.BAD_REQUEST, "OTP가 만료되었습니다."),
    OTP_MAX_ATTEMPTS(HttpStatus.BAD_REQUEST, "OTP 시도 횟수를 초과했습니다."),
    OTP_NOT_VERIFIED(HttpStatus.BAD_REQUEST, "OTP 검증이 필요합니다."),
    OTP_REQUIRED(HttpStatus.UNAUTHORIZED, "OTP 입력이 필요합니다."),

    // ================= Auth =================
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "인증이 필요합니다."), //토큰이 없을 경우
    FORBIDDEN(HttpStatus.FORBIDDEN, "권한이 없습니다."),

    // Token 관련
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다."),
    ACCESS_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "Access Token이 만료되었습니다."), // 401
    INVALID_REFRESH_TOKEN(HttpStatus.BAD_REQUEST, "유효하지 않거나 만료된 Refresh Token입니다."),

    LOGIN_REQUIRED(HttpStatus.UNAUTHORIZED, "로그인이 필요한 기능입니다."),
    INVALID_AUTH_CODE(HttpStatus.BAD_REQUEST,"유효하지 않거나 만료된 코드입니다."),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "해당 리소스에 대한 접근 권한이 없습니다."),

    // ================= Member =================
    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "회원 정보를 찾을 수 없습니다."),
    RESIDENT_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 입주민 정보가 없습니다."),
    DUPLICATE_LOGIN_ID(HttpStatus.CONFLICT, "이미 존재하는 아이디입니다."),
    SOCIAL_LOGIN_RESTRICTED(HttpStatus.CONFLICT, "소셜 로그인 회원은 비밀번호를 변경할 수 없습니다."),
    MEMBER_LOGIN_FAILED(HttpStatus.UNAUTHORIZED, "아이디 또는 비밀번호가 올바르지 않습니다."),

    // ================= Resident =================
    RESIDENT_DUPLICATED(HttpStatus.CONFLICT, "이미 등록된 입주민(휴대폰 번호)입니다."),

    // ================= ManagementFee =================
    MANAGEMENT_FEE_NOT_FOUND(HttpStatus.NOT_FOUND, "관리비 항목을 찾을 수 없습니다."),
    DUPLICATE_MANAGEMENT_FEE_NAME(HttpStatus.CONFLICT, "이미 존재하는 관리비 항목명입니다."),
    MANAGEMENT_FEE_INACTIVE(HttpStatus.BAD_REQUEST, "비활성화된 관리비 항목입니다."),
    MANAGEMENT_FEE_ALREADY_ACTIVE(HttpStatus.BAD_REQUEST, "이미 활성화된 관리비 항목입니다."),
    MANAGEMENT_FEE_RESTORE_CONFLICT(HttpStatus.CONFLICT, "동일한 이름의 활성 관리비 항목이 존재하여 복구할 수 없습니다."),

    // ================= Reservation ================
    RESERVATION_TIME_OVERLAPPED(HttpStatus.CONFLICT, "해당 시간에 이미 예약이 존재합니다."),

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
