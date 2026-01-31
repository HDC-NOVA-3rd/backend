package com.backend.nova.apartmentbilllog.repository;

import com.backend.nova.apartmentbilllog.entity.Bill;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BillRepository extends JpaRepository<Bill, Long> {

    Optional<Bill> findByBillUuid(UUID billUuid);

    // 사용자: 세대(Ho)별 고지서 조회
    List<Bill> findByHo_Id(Long hoId);

    // 관리자: 단지(Apartment)별 전체 고지서 조회
    List<Bill> findByHo_Apartment_Id(Long apartmentId);

    // 관리자: 단지 내 특정 고지서 상세 조회
    Optional<Bill> findByIdAndHo_Apartment_Id(Long id, Long apartmentId);

    // 사용자: 세대 내 특정 고지서 상세 조회
    Optional<Bill> findByIdAndHo_Id(Long id, Long hoId);
}