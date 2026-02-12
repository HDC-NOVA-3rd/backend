package com.backend.nova.global.notification;
import com.niamedtech.expo.exposerversdk.request.PushNotification;
import lombok.Builder;

import java.util.Collections;
import java.util.Map;

/**
 * 알림 전송을 위한 공통 DTO
 * 라이브러리 의존성을 제거하고 순수 데이터만 전달합니다.
 */
@Builder
public record PushMessageRequest(
        String to,      // 수신자 토큰 (단일 String)
        String title,   // 알림 제목
        String body,     // 알림 내용
        Map<String, Object> data // 화면 이동이나 로직 처리에 필요한 데이터
) {
    public PushNotification toNotify(){
        PushNotification notification = new PushNotification();
        notification.setTo(Collections.singletonList(to));
        notification.setTitle(title);
        notification.setBody(body);
        notification.setSound("default"); // 소리는 기본값 (추후 DTO에 필드 추가 가능)
        if (data != null && !data.isEmpty()) {
            notification.setData(data);
        }
        return notification;
    }
}
