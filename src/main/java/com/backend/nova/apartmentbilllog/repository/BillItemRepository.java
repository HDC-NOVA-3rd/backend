package com.backend.nova.apartmentbilllog.repository;

import com.backend.nova.apartmentbilllog.entity.BillItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BillItemRepository extends JpaRepository<BillItem, Long> {
}