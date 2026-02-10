package com.backend.nova.global.exception;

import lombok.Getter;
import org.springframework.security.core.AuthenticationException;


// Authentication Error 발생 시 처리되는 Custom Exception
@Getter
public class CustomAuthenticationException extends AuthenticationException {
    private final ErrorCode errorCode;

    public CustomAuthenticationException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}
