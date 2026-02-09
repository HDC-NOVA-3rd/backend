package com.backend.nova.bill.repository;

import com.backend.nova.bill.entity.Bill;
import com.backend.nova.bill.entity.BillStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BillRepository extends JpaRepository<Bill, Long> {

    // 호,월별로 고지서 조회
    boolean existsByHo_IdAndMonth(Long hoId, String month);
    // 호,월별로 고지서 조회
    Optional<Bill> findByHo_IdAndMonth(Long hoId, String month);

    // 사용자: 거주세대(Ho) 고지서 조회
    List<Bill> findByHo_Id(Long hoId);

    // 관리자: 관리단지(Apartment) 전체 고지서 조회
    List<Bill> findByHo_Dong_Apartment_Id(Long apartmentId);

    // 관리자: 단지 내 특정 고지서 상세 조회
    Optional<Bill> findByIdAndHo_Dong_Apartment_Id(Long id, Long apartmentId);

    // 사용자: 세대 내 특정 고지서 상세 조회
    Optional<Bill> findByIdAndHo_Id(Long id, Long hoId);

    Optional<Bill> findByBillUuid(UUID billUuid);

    Optional<Bill> findByBillUuidAndHoId(UUID billUuid, Long hoId);

    Optional<Bill> findByBillUuidAndHo_Apartment_Id(UUID billUuid, Long apartmentId);

    //세대별 미납 조회(입주민 화면)
    List<Bill> findByHo_IdAndStatus(Long hoId, BillStatus status);

    //아파트 단지 미납 전체 조회(관리자)
    List<Bill> findByHo_Dong_Apartment_IdAndStatus(
            Long apartmentId,
            BillStatus status
    );

    //특정 월 미납 (운영
    List<Bill> findByHo_Dong_Apartment_IdAndMonthAndStatus(
            Long apartmentId,
            String month,
            BillStatus status
    );


    //입주민 확정전 고지서 상시 조회
    List<Bill> findByHo_IdAndStatusIn(
            Long hoId,
            List<BillStatus> statuses
    );



}