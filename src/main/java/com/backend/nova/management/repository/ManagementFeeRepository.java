package com.backend.nova.management.repository;

import com.backend.nova.management.entity.ManagementFee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ManagementFeeRepository extends JpaRepository<ManagementFee, Long> {

    List<ManagementFee> findByApartmentId(Long apartmentId); // 전체 항목 조회
    List<ManagementFee> findByApartmentIdAndActiveTrue(Long apartmentId); // 활성 항목만 조회
    List<ManagementFee> findByApartmentIdAndActiveFalse(Long apartmentId); // 삭제만 조회

    boolean existsByApartmentIdAndNameAndActiveTrue(Long apartmentId, String name);

    //단지 관리비 항목 조회 (활성만)
    List<ManagementFee> findByApartment_IdAndActiveTrue(Long apartmentId);
}
