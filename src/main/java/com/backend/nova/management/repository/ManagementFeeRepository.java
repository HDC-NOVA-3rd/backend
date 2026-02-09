package com.backend.nova.management.repository;

import com.backend.nova.management.entity.ManagementFee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ManagementFeeRepository extends JpaRepository<ManagementFee, Long> {

    // 단지 ID로 활성 항목만 조회
    List<ManagementFee> findByApartmentIdAndActiveTrue(Long apartmentId);

}
