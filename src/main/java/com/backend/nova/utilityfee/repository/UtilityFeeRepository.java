package com.backend.nova.utilityfee.repository;

import com.backend.nova.utilityfee.entity.UtilityFee;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.YearMonth;

public interface UtilityFeeRepository extends JpaRepository<UtilityFee, Long> {

    boolean existsByHo_Apartment_IdAndMonth(Long apartmentId, YearMonth month);

}