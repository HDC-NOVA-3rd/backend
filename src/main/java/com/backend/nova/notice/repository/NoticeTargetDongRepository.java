package com.backend.nova.notice.repository;

import com.backend.nova.notice.entity.Notice;
import com.backend.nova.notice.entity.NoticeTargetDong;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface NoticeTargetDongRepository extends JpaRepository<NoticeTargetDong, Long> {

    @Query("select ntd.dong.id from NoticeTargetDong ntd where ntd.notice.id = :noticeId")
    List<Long> findDongIdsByNoticeId(Long noticeId);

    @Query("select ntd.notice from NoticeTargetDong ntd where ntd.dong.id = :dongId order by ntd.notice.createdAt desc")
    List<Notice> findNoticesByDongIdOrderByCreatedAtDesc(Long dongId);

    void deleteAllByNoticeId(Long noticeId);
}
