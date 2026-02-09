package com.backend.nova.notice.repository;

import com.backend.nova.notice.entity.NoticeSendLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NoticeSendLogRepository extends JpaRepository<NoticeSendLog, Long> {
    List<NoticeSendLog> findAllByOrderBySentAtDesc();

    void deleteAllByNoticeId(Long noticeId);
}
