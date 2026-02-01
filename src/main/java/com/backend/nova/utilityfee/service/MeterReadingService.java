package com.backend.nova.utilityfee.service;

import com.backend.nova.utilityfee.dto.MeterReadingResponse;
import com.backend.nova.utilityfee.entity.MeterReading;
import com.backend.nova.utilityfee.repository.MeterReadingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MeterReadingService {

    private final MeterReadingRepository meterReadingRepository;

    // =============================
    // 단지 전체 미터기 조회 (관리자)
    // =============================
    public List<MeterReadingResponse> getMeterReadingsByApartment(Long apartmentId) {
        return meterReadingRepository.findByHo_Apartment_Id(apartmentId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // =============================
    // 개별 미터기 상세 조회 (관리자)
    // =============================
    public MeterReadingResponse getMeterReading(Long meterReadingId, Long apartmentId) {
        MeterReading meterReading =
                meterReadingRepository.findByIdAndHo_Apartment_Id(meterReadingId, apartmentId)
                        .orElseThrow(() -> new IllegalArgumentException("MeterReading not found"));

        return toResponse(meterReading);
    }

    // =============================
    // Entity → DTO
    // =============================
    private MeterReadingResponse toResponse(MeterReading meterReading) {
        return MeterReadingResponse.builder()
                .id(meterReading.getId())
                .hoId(meterReading.getHo().getId())
                .billingMonth(meterReading.getBillingMonth())
                .items(
                        meterReading.getItems().stream()
                                .map(item -> MeterReadingResponse.Item.builder()
                                        .id(item.getId())
                                        .name(item.getName())
                                        .category(item.getCategory())
                                        .price(item.getPrice())
                                        .build())
                                .collect(Collectors.toList())
                )
                .build();
    }
}
