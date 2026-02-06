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
    HO_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 호 정보를 찾을 수 없습니다."),

    // ================= Notice =================
    NOTICE_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 공지를 찾을 수 없습니다."),


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
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다."),


    // ==================== Member ==============
    /* 400 BAD_REQUEST : 잘못된 요청 */
    INVALID_REFRESH_TOKEN(HttpStatus.BAD_REQUEST,  "유효하지 않거나 만료된 Refresh Token입니다."),

    /* 401 UNAUTHORIZED : 인증 실패 (로그인 필요) */
    LOGIN_REQUIRED(HttpStatus.UNAUTHORIZED,  "로그인이 필요한 기능입니다."),

    /* 404 NOT_FOUND : 리소스를 찾을 수 없음 */
    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND,  "회원 정보를 찾을 수 없습니다."),
    RESIDENT_NOT_FOUND(HttpStatus.NOT_FOUND,  "해당 입주민 정보가 없습니다."),

    /* 409 CONFLICT : 중복된 리소스 */
    DUPLICATE_LOGIN_ID(HttpStatus.CONFLICT,  "이미 존재하는 아이디입니다."),
    SOCIAL_LOGIN_RESTRICTED(HttpStatus.CONFLICT,  "소셜 로그인 회원은 비밀번호를 변경할 수 없습니다."),

    // ================= Resident (신규 추가) =================
    /* 409 CONFLICT : 중복된 리소스 */
    RESIDENT_DUPLICATED(HttpStatus.CONFLICT, "이미 등록된 입주민(휴대폰 번호)입니다.");

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
