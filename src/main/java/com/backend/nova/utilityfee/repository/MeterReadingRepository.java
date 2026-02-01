package com.backend.nova.utilityfee.repository;

import com.backend.nova.utilityfee.entity.MeterReading;
import com.backend.nova.utilityfee.entity.MeterType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MeterReadingRepository extends JpaRepository<MeterReading, Long> {

    Optional<MeterReading> findByBillUuid(UUID billUuid);

    // 사용자: 세대(Ho)별 현재 고지서 조회
    List<MeterReading> findByHo_Id(Long hoId);

    // 관리자: 단지(Apartment)별 현재 고지서 조회
    List<MeterReading> findByHo_Apartment_Id(Long apartmentId);

    // 관리자: 단지 내 특정 현재 고지서 상세 조회
    Optional<MeterReading> findByIdAndHo_Apartment_Id(Long id, Long apartmentId);

    // 사용자: 세대 내 특정 현재 고지서 상세 조회
    Optional<MeterReading> findByIdAndHo_Id(Long id, Long hoId);

    List<MeterReading> findByHoAndMeterTypeAndMonth(
            Long hoId,
            MeterType meterType,
            YearMonth month
    );

}