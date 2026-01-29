package com.backend.nova.apartment.repository;

import com.backend.nova.apartment.entity.Dong;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DongRepository extends JpaRepository<Dong, Long> {
    // 아파트 ID로 동 리스트 조회
    List<Dong> findAllByApartmentId(Long apartmentId);
}
