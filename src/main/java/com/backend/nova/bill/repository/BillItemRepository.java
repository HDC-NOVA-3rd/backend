package com.backend.nova.bill.repository;

import com.backend.nova.bill.entity.BillItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BillItemRepository extends JpaRepository<BillItem, Long> {
}