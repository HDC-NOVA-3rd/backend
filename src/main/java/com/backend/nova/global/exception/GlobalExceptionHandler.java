package com.backend.nova.global.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<?> handleBusinessException(BusinessException e) {
        ErrorCode errorCode = e.getErrorCode();

        return ResponseEntity
                .status(errorCode.getStatus())
                .body(
                        ErrorResponse.of(errorCode)
                );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleException(Exception e) {
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
