package com.backend.nova.chat.service;

import com.backend.nova.chat.dto.ChatMessageResponse;
import com.backend.nova.chat.dto.ChatSessionSummaryResponse;
import com.backend.nova.chat.entity.ChatMessage;
import com.backend.nova.chat.entity.ChatSession;
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

   public List<ChatSessionSummaryResponse> getSessions(Long memberId) {
        return chatSessionRepository
                .findByMember_IdAndDeletedAtIsNullOrderByLastMessageAtDesc(memberId)
                .stream()
                .map(session -> {
                    ChatMessage lastMessage =
                            chatMessageRepository
                                    .findTopByChatSession_SessionIdOrderByCreatedAtDesc(session.getSessionId())
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

    public List<ChatMessageResponse> getMessages(Long memberId, String sessionId) {
        chatSessionRepository.findBySessionIdAndMember_IdAndDeletedAtIsNull(sessionId, memberId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않거나 삭제된 세션"));

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

    @Transactional
    public void deleteSession(Long memberId, String sessionId) {
        var session = chatSessionRepository
                .findBySessionIdAndMember_IdAndDeletedAtIsNull(sessionId, memberId)
                .orElseThrow(() -> new IllegalArgumentException("세션이 없거나 이미 삭제됨"));

        session.softDelete();
    }

    @Transactional
    public void deleteAllSessions(Long memberId) {
        var sessions = chatSessionRepository
                .findByMember_IdAndDeletedAtIsNullOrderByLastMessageAtDesc(memberId);

        sessions.forEach(ChatSession::softDelete);
    }


}

