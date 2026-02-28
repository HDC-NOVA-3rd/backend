package com.backend.nova.bill.repository;

import com.backend.nova.bill.entity.Bill;
import com.backend.nova.bill.entity.BillStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface BillRepository extends JpaRepository<Bill, Long> {

    Optional<Bill> findByBillUid(String billUid);

    // 사용자: 세대 내 특정 고지서 상세 조회
    Optional<Bill> findByIdAndHo_Id(Long id, Long hoId);

    boolean existsByHo_Dong_Apartment_IdAndBillMonth(Long apartmentId, String month);

    List<Bill> findByHo_Dong_Apartment_IdAndBillMonth(Long apartmentId, String month);

    // 마감일이 어제(혹은 그 이전)이고, 아직 READY(미납) 상태인 고지서 조회
    List<Bill> findByStatusAndDueDateBefore(BillStatus status, LocalDate date);

    // 상세 조회 (보안을 위해 apartmentId/hoId 조건 추가 버전 필요)
    @Query("""
        select distinct b from Bill b
        join fetch b.ho h
        join fetch h.dong d
        join fetch d.apartment a
        left join fetch b.items
        where b.id = :billId and a.id = :apartmentId
    """)
    Optional<Bill> findDetailForAdmin(Long billId, Long apartmentId);

    @Query("""
        select distinct b from Bill b
        join fetch b.ho h
        join fetch h.dong d
        join fetch d.apartment a
        left join fetch b.items
        where b.id = :billId and h.id = :hoId
    """)
    Optional<Bill> findDetailForMember(Long billId, Long hoId);

}