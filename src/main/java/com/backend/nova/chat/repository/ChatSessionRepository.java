package com.backend.nova.chat.repository;

import com.backend.nova.chat.entity.ChatSession;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatSessionRepository extends JpaRepository<ChatSession, String> {

}
