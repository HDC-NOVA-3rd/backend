package com.backend.nova.notice.repository;

import com.backend.nova.notice.entity.Notice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface NoticeRepository extends JpaRepository<Notice, Long> {

    @Query("""
            select distinct n
            from Notice n
            left join NoticeTargetDong ntd on ntd.notice = n
            where n.admin.apartment.id = :apartmentId
              and (n.targetScope = 'ALL' or ntd.dong.id = :dongId)
            order by n.createdAt desc
            """)
    List<Notice> findBoardNotices(Long apartmentId, Long dongId);

    @Query("""
            select n
            from Notice n
            where n.admin.apartment.id = :apartmentId
            order by n.createdAt desc
            """)
    List<Notice> findBoardNoticesForApartment(Long apartmentId);
}
