package com.backend.nova.chat.repository;

import com.backend.nova.chat.dto.ChatSessionSummaryResponse;
import com.backend.nova.chat.entity.ChatSession;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface ChatSessionRepository extends JpaRepository<ChatSession, String> {

    List<ChatSession> findByResident_IdOrderByLastMessageAtDesc(Long residentId);
}

