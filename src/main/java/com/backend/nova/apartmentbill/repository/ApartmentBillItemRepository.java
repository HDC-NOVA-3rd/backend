package com.backend.nova.apartmentbill.repository;

import com.backend.nova.apartmentbill.entity.ApartmentBillItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ApartmentBillItemRepository extends JpaRepository<ApartmentBillItem, Long> {

    // 단지 ID로 활성 항목만 조회
    List<ApartmentBillItem> findByApartmentIdAndActiveTrue(Long apartmentId);

}
