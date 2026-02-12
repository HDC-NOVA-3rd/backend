package com.backend.nova.safety.controller;

import com.backend.nova.global.notification.NotificationService;
import com.backend.nova.global.notification.PushMessageRequest;
import com.backend.nova.member.repository.MemberRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Tag(name = "Safety", description = "Safety API")
@RestController
@RequestMapping("/api/alert")
@RequiredArgsConstructor
public class SafetyAlertController {

    private final NotificationService notificationService;
    private final MemberRepository memberRepository;

    @Operation(summary = "화재/위험 긴급 알림 발송", description = "아파트 전체 거주자에게 푸시 알림을 발송합니다.")
    @PostMapping("/safety")
    public ResponseEntity<Void> sendSafetyAlert(@RequestBody Map<String, Object> data) {
        Long apartmentId = Long.parseLong(data.get("apartmentId").toString());
        String title = (String) data.get("title");
        String message = (String) data.get("message");
        String type = (String) data.get("type");

        // 아파트의 모든 멤버 조회 (푸시 토큰이 있는 경우만)
        // Member → Resident → Ho → Dong → Apartment
        memberRepository.findAll().stream()
                .filter(m -> m.getResident() != null
                        && m.getResident().getHo() != null
                        && m.getResident().getHo().getDong() != null
                        && m.getResident().getHo().getDong().getApartment() != null
                        && m.getResident().getHo().getDong().getApartment().getId().equals(apartmentId))
                .filter(m -> m.getPushToken() != null && !m.getPushToken().isBlank())
                .forEach(m -> {
                    List<PushMessageRequest> messages = new ArrayList<>();
                    messages.add(PushMessageRequest.builder()
                            .to(m.getPushToken())
                            .title(title)
                            .body(message)
                            .data(Map.of("type", type, "category", "SAFETY"))
                            .build());
                    notificationService.sendPushMessages(messages);
                });

        return ResponseEntity.ok().build();
    }
}
