package com.backend.nova.chat.repository;

import com.backend.nova.chat.entity.ChatMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, String> {
    // 최신 N개 (최신순)
    List<ChatMessage> findByChatSession_SessionIdOrderByCreatedAtDesc(String sessionId, Pageable pageable);
}
