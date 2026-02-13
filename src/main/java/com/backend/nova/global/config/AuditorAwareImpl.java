package com.backend.nova.global.config;

import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class AuditorAwareImpl implements AuditorAware<Long> {

    //JWT 필터에서 SecurityContextHolder에 저장한 유저 정보를 꺼내오기
    @Override
    public Optional<Long> getCurrentAuditor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated() ||
                authentication.getPrincipal().equals("anonymousUser")) {
            return Optional.empty();
        }

        // JWT 인증 후 SecurityContext에 저장된 Principal(유저 PK)을 반환
        // 보통 유저 ID(Long)를 담도록 설계되므로 이에 맞춰 캐스팅합니다.
        try {
            return Optional.of(Long.parseLong(authentication.getName()));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }
}