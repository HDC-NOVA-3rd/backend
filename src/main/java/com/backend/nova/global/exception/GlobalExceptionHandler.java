package com.backend.nova.global.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 비즈니스 예외 (도메인 규칙 위반)
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException e) {
        ErrorCode errorCode = e.getErrorCode();

        return ResponseEntity
                .status(errorCode.getStatus())
                .body(ErrorResponse.of(errorCode));
    }

    /**
     * 요청값 검증 실패 (@Valid)
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            MethodArgumentNotValidException e
    ) {
        return ResponseEntity
                .badRequest()
                .body(
                        ErrorResponse.of(
                                ErrorCode.INVALID_REQUEST   // 400
                        )
                );
    }

    /**
     * 인증 실패 (로그인 실패, 비밀번호 불일치 등)
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthenticationException(
            AuthenticationException e
    ) {
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(
                        ErrorResponse.of(
                                ErrorCode.UNAUTHORIZED      // 401
                        )
                );
    }

    /**
     * 그 외 모든 예외 (최후의 보루)
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception e) {
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
