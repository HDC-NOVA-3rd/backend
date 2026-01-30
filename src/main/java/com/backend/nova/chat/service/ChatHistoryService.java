package com.backend.nova.chat.service;

import com.backend.nova.chat.dto.ChatMessageResponse;
import com.backend.nova.chat.dto.ChatSessionSummaryResponse;
import com.backend.nova.chat.entity.ChatMessage;
import com.backend.nova.chat.repository.ChatMessageRepository;
import com.backend.nova.chat.repository.ChatSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly=true)
public class ChatHistoryService {
    private final ChatSessionRepository chatSessionRepository;
    private final ChatMessageRepository chatMessageRepository;

    public List<ChatSessionSummaryResponse> getSessions(Long residentId) {

        return chatSessionRepository
                .findByResident_IdOrderByLastMessageAtDesc(residentId)
                .stream()
                .map(session -> {
                    // 마지막 메시지 1건 조회
                    ChatMessage lastMessage =
                            chatMessageRepository
                                    .findTopByChatSession_SessionIdOrderByCreatedAtDesc(
                                            session.getSessionId()
                                    )
                                    .orElse(null);

                    return new ChatSessionSummaryResponse(
                            session.getSessionId(),
                            lastMessage != null ? lastMessage.getContent() : "",
                            session.getLastMessageAt(),
                            session.getStatus()
                    );
                })
                .toList();
    }
    public List<ChatMessageResponse> getMessages(String sessionId) {

        chatSessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 세션"));

        return chatMessageRepository
                .findByChatSession_SessionIdOrderByCreatedAtAsc(sessionId)
                .stream()
                .map(m -> new ChatMessageResponse(
                        m.getRole().name(),
                        m.getContent(),
                        m.getCreatedAt()
                ))
                .toList();
    }

}