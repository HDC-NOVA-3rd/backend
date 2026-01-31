package com.backend.nova.apartmentcurrentbill.repository;

import com.backend.nova.apartmentcurrentbill.entity.CurrentBillItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CurrentBillItemRepository extends JpaRepository<CurrentBillItem, Long> {
}