package com.backend.nova.apartment.repository;

import com.backend.nova.apartment.entity.Ho;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HoRepository extends JpaRepository<Ho, Long> {
    // 동 ID로 호 리스트 조회
    List<Ho> findAllByDongId(Long dongId);

    // 단지 내 모든 세대 리스트 조회
    List<Ho> findByDong_Apartment_Id(Long apartmentId);

}
