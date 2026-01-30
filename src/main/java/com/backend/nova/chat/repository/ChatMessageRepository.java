package com.backend.nova.chat.repository;

import com.backend.nova.chat.entity.ChatMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    // 최신 N개 (최신순)
    List<ChatMessage> findByChatSession_SessionIdOrderByCreatedAtDesc(String sessionId, Pageable pageable);
    Optional<ChatMessage> findTopByChatSession_SessionIdOrderByCreatedAtDesc(String sessionId);


    List<ChatMessage> findByChatSession_SessionIdOrderByCreatedAtAsc(String sessionId);
}
