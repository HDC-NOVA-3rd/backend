package com.backend.nova.notice.repository;

import com.backend.nova.notice.entity.NoticeSendLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface NoticeSendLogRepository extends JpaRepository<NoticeSendLog, Long> {
    List<NoticeSendLog> findAllByOrderBySentAtDesc();

    void deleteAllByNoticeId(Long noticeId);

    // 공지 처리 시 상세 조회
    Optional<NoticeSendLog> findByNotice_IdAndRecipientId(Long noticeId, Long recipientId);
    //공지 상세 열람 시 읽음처리
    List<NoticeSendLog> findByRecipientIdAndNotice_IdIn(Long recipientId, Collection<Long> noticeIds);

}
