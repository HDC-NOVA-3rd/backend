package com.backend.nova.utilityfee.service;

import com.backend.nova.apartment.entity.Ho;
import com.backend.nova.apartment.repository.HoRepository;
import com.backend.nova.utilityfee.entity.MeterReading;
import com.backend.nova.utilityfee.entity.MeterType;
import com.backend.nova.utilityfee.entity.UtilityFee;
import com.backend.nova.utilityfee.repository.MeterReadingRepository;
import com.backend.nova.utilityfee.repository.UtilityFeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class UtilityFeeClosingService {

    private final HoRepository hoRepository;
    private final MeterReadingRepository meterReadingRepository;
    private final UtilityFeeRepository utilityFeeRepository;

    // =============================
    // 월 마감 처리
    // =============================
    public void closeMonth(Long apartmentId, YearMonth month) {

        // 1. 이미 마감됐는지 체크
        if (utilityFeeRepository.existsByHo_Apartment_IdAndMonth(apartmentId, month)) {
            throw new IllegalStateException("이미 마감된 월입니다.");
        }

        // 2. 단지 내 모든 세대 조회
        List<Ho> hos = hoRepository.findByApartment_Id(apartmentId);

        for (Ho ho : hos) {
            for (MeterType meterType : MeterType.values()) {

                // 3. 해당 월 검침 데이터 조회
                List<MeterReading> readings =
                        meterReadingRepository.findByHoAndMeterTypeAndMonth(
                                ho.getId(),
                                meterType,
                                month
                        );

                if (readings.isEmpty()) {
                    continue; // 검침 없으면 스킵
                }

                // 4. 사용량 계산 (max - min)
                long usage = calculateUsage(readings);

                // 5. 요금 계산
                int fee = calculateFee(meterType, usage);

                // 6. UtilityFee 생성
                UtilityFee utilityFee = UtilityFee.builder()
                        .ho(ho)
                        .meterType(meterType)
                        .usage(usage)
                        .calculatedFee(fee)
                        .month(month)
                        .calculationBasis(meterType.name() + "_V1")
                        .build();

                utilityFeeRepository.save(utilityFee);
            }
        }
    }

    // =============================
    // 사용량 계산
    // =============================
    private long calculateUsage(List<MeterReading> readings) {
        long min = readings.stream()
                .min(Comparator.comparingLong(MeterReading::getReadingValue))
                .get()
                .getReadingValue();

        long max = readings.stream()
                .max(Comparator.comparingLong(MeterReading::getReadingValue))
                .get()
                .getReadingValue();

        return max - min;
    }

    // =============================
    // 요금 계산 (임시 로직)
    // =============================
    private int calculateFee(MeterType meterType, long usage) {
        return switch (meterType) {
            case WATER -> (int) usage * 800;
            case ELECTRIC -> (int) usage * 120;
            case GAS -> (int) usage * 900;
        };
    }
}
