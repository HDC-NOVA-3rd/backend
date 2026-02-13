package com.backend.nova.global.notification;
import com.niamedtech.expo.exposerversdk.*;
import com.niamedtech.expo.exposerversdk.request.PushNotification;
import com.niamedtech.expo.exposerversdk.response.Status;
import com.niamedtech.expo.exposerversdk.response.TicketResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
public class NotificationService {

    // 1. [기본] 데이터(Data)가 포함된 알림 전송 메서드 ( 모바일 redirect 용도)
    public PushMessageRequest sendNotification(String pushToken, String title, String body, Map<String, Object> data) {
        if (pushToken != null && !pushToken.isBlank()) {

            // 빌더 시작
            PushMessageRequest.PushMessageRequestBuilder builder = PushMessageRequest.builder()
                    .to(pushToken)
                    .title(title)
                    .body(body);

            // 데이터가 존재할 경우에만 세팅
            if (data != null && !data.isEmpty()) {
                builder.data(data);
            }

            // 전송
            return builder.build();
        }
        return null;
    }

    // 2. 데이터가 필요 없는 경우를 위한 메서드
    public PushMessageRequest sendNotification(String pushToken, String title, String body) {
        // data에 null을 넘겨서 위 메서드를 호출
        return sendNotification(pushToken, title, body, null);
    }

    /**
     * [배치 전송] 여러 건의 알림을 한 번에 전송
     * - 자동으로 100개씩 쪼개서(Chunking) 전송함
     * - 스케줄러에서 사용하기 적합
     */
    @Async
    public CompletableFuture<Void> sendPushMessages(List<PushMessageRequest> messageDtos) {
        log.info("현재 실행 중인 스레드: {}", Thread.currentThread().getName());
        if (messageDtos == null || messageDtos.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }
        // 1. DTO -> Library Object(PushNotification) 변환
        List<PushNotification> expoNotifications = messageDtos.stream()
                .map(PushMessageRequest::toNotify)
                .toList();

        // 2. 전송 로직 (라이브러리 사용)
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {

            ExpoPushNotificationClient client = ExpoPushNotificationClient.builder()
                    .setHttpClient(httpClient)
                    .build();

            // 2. 전송 (SDK가 내부적으로 청크(Chunk) 처리를 하여 전송)
            List<TicketResponse.Ticket> responseTickets = client.sendPushNotifications(expoNotifications);

            // 3. 결과 확인 (Ticket Response 처리)
            int successCount = 0;
            for (TicketResponse.Ticket ticket : responseTickets) {
                // Status가 OK인지 ERROR인지 확인
                if (ticket.getStatus() == Status.OK) {
                    log.info("푸시 전송 성공 ID: {}", ticket.getId());
                    successCount++;
                } else {
                    // 에러 발생 시 상세 내용 로깅
                    log.error("푸시 전송 실패: {}", ticket.getMessage());
                    if (ticket.getDetails() != null) {
                        log.error("에러 상세: error={}", ticket.getDetails().getError());
                    }
                }
            }
            log.info("총 {}건 중 {}건 전송 성공", messageDtos.size(), successCount);

        } catch (Exception e) {
            log.error("Expo 푸시 전송 중 시스템 예외 발생", e);
        }

        return CompletableFuture.completedFuture(null);
    }
}
