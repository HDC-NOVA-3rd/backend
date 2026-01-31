package com.backend.nova.apartmentcurrentbill.repository;

import com.backend.nova.apartmentcurrentbill.entity.CurrentBill;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CurrentBillRepository extends JpaRepository<CurrentBill, Long> {

    Optional<CurrentBill> findByBillUuid(UUID billUuid);

    // 사용자: 세대(Ho)별 현재 고지서 조회
    List<CurrentBill> findByHo_Id(Long hoId);

    // 관리자: 단지(Apartment)별 현재 고지서 조회
    List<CurrentBill> findByHo_Apartment_Id(Long apartmentId);

    // 관리자: 단지 내 특정 현재 고지서 상세 조회
    Optional<CurrentBill> findByIdAndHo_Apartment_Id(Long id, Long apartmentId);

    // 사용자: 세대 내 특정 현재 고지서 상세 조회
    Optional<CurrentBill> findByIdAndHo_Id(Long id, Long hoId);
}