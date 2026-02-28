package com.backend.nova.chat.repository;

import com.backend.nova.chat.entity.ChatSession;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ChatSessionRepository extends JpaRepository<ChatSession, String> {


    // member
    List<ChatSession> findByMember_IdAndDeletedAtIsNullOrderByLastMessageAtDesc(Long memberId);

    // "이 memberId의 이 sessionId"가 맞는지 검증 + 삭제 제외
    java.util.Optional<ChatSession> findBySessionIdAndMember_IdAndDeletedAtIsNull(String sessionId, Long memberId);

}

