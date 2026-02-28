package com.backend.nova.global.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * 비즈니스 예외 (도메인 규칙 위반)
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException e, HttpServletRequest request) {
        ErrorCode errorCode = e.getErrorCode();
        // 스택 트레이스 없이 URI와 에러 메시지만 WARN으로 남김
        log.warn("BusinessException 발생 [{}]: {}", request.getRequestURI(), errorCode.getMessage());
        return ResponseEntity
                .status(errorCode.getStatus())
                .body(ErrorResponse.of(errorCode));
    }

    /**
     * 요청값 검증 실패 (@Valid)
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            MethodArgumentNotValidException e, HttpServletRequest request
    ) {
        log.warn("ValidationException 발생 [{}]: {}", request.getRequestURI(), e.getMessage());
        return ResponseEntity
                .badRequest()
                .body(
                        ErrorResponse.of(
                                ErrorCode.INVALID_REQUEST   // 400
                        )
                );
    }

    // 1. 중복 예약 등 상태 충돌 예외 -> 409 Conflict
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalStateException(IllegalStateException e, HttpServletRequest request) {
        log.warn("IllegalStateException 발생 [{}]: {}", request.getRequestURI(), e.getMessage());
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(
                        ErrorResponse.of(
                                ErrorCode.RESERVATION_TIME_OVERLAPPED   // 409
                        )
                );
    }

    // 2. 잘못된 인원수, 시간 설정 등 -> 400 Bad Request
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException e, HttpServletRequest request) {
        log.warn("IllegalArgumentException 발생 [{}]: {}", request.getRequestURI(), e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponse("400",e.getMessage()));
    }

    /**
     * 인증 실패 (로그인 실패, 비밀번호 불일치 등)
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthenticationException(AuthenticationException e, HttpServletRequest request) {
        log.warn("AuthenticationException 발생 [{}]: {}", request.getRequestURI(), e.getMessage());
        // 1. 우리가 만든 예외(ErrorCode 포함)인 경우
        if(e instanceof CustomAuthenticationException){
            ErrorCode errorCode = ((CustomAuthenticationException) e).getErrorCode();
            return ResponseEntity
                    .status(errorCode.getStatus())
                    .body(ErrorResponse.of(errorCode));
        }
        // 2. 그 외 Spring Security 기본 예외 (자격 증명 없음 등)
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(ErrorResponse.of(ErrorCode.UNAUTHORIZED));      // 401
    }

    /**
     * 그 외 모든 예외 (최후의 보루)
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception e, HttpServletRequest request) {
        log.error("Unhandled Exception 발생 [{}]: ", request.getRequestURI(), e);
        return ResponseEntity
                .status(ErrorCode.INTERNAL_SERVER_ERROR.getStatus())
                .body(
                        ErrorResponse.of(ErrorCode.INTERNAL_SERVER_ERROR)
                );
    }

//    @ExceptionHandler(InvalidOtpException.class)
//    public ResponseEntity<String> handleInvalidOtp(InvalidOtpException e) {
//        return ResponseEntity
//                .status(HttpStatus.BAD_REQUEST)
//                .body(e.getMessage());
//    }
//
//    @ExceptionHandler(UnauthorizedException.class)
//    public ResponseEntity<String> handleUnauthorized(UnauthorizedException e) {
//        return ResponseEntity
//                .status(HttpStatus.UNAUTHORIZED)
//                .body(e.getMessage());
//    }
}
