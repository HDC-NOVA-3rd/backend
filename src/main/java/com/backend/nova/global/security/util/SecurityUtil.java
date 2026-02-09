package com.backend.nova.global.security.util;

import com.backend.nova.auth.admin.AdminDetails;
import com.backend.nova.auth.member.MemberDetails;
import com.backend.nova.global.exception.BusinessException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityUtil {

    public static boolean isAdmin() {
        return getCurrentUserDetails() instanceof AdminDetails;
    }

    public static boolean isMember() {
        return getCurrentUserDetails() instanceof MemberDetails;
    }

    public static void validateAdmin() {
        if (!isAdmin()) {
            throw new BusinessException(FORBIDDEN, "관리자만 접근 가능합니다.");
        }
    }

    public static void validateMember() {
        if (!isMember()) {
            throw new BusinessException(FORBIDDEN, "입주민만 접근 가능합니다.");
        }
    }

    public static Long getCurrentAdminApartmentId() {
        validateAdmin();
        return ((AdminDetails) getCurrentUserDetails()).getApartmentId();
    }

    public static Long getCurrentMemberHoId() {
        validateMember();
        return ((MemberDetails) getCurrentUserDetails()).getHoId();
    }

    private static Object getCurrentUserDetails() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new BusinessException(UNAUTHENTICATED);
        }
        return auth.getPrincipal();
    }
}
