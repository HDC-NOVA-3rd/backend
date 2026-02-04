package com.backend.nova.member.dto;

import lombok.Builder;

@Builder
public record ResetPWResponse(
        String tempPassword, // 화면에 보여줄 임시 비밀번호
        String message      // 안내 메시지
) {
}
