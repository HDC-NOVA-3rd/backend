package com.backend.nova.utilityfee.service;

import com.backend.nova.apartment.entity.Ho;
import com.backend.nova.apartment.repository.HoRepository;
import com.backend.nova.utilityfee.dto.UtilityFeeRequest;
import com.backend.nova.utilityfee.dto.UtilityFeeResponse;
import com.backend.nova.utilityfee.entity.MeterReading;
import com.backend.nova.utilityfee.entity.UtilityFee;
import com.backend.nova.utilityfee.repository.MeterReadingRepository;
import com.backend.nova.utilityfee.repository.UtilityFeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class UtilityFeeService {

    private final UtilityFeeRepository utilityFeeRepository;
    private final MeterReadingRepository meterReadingRepository;
    private final HoRepository hoRepository;

    // =============================
    // 테스트용 공과금 생성 (운영 제거)
    // =============================
    public UtilityFeeResponse createUtilityFee(UtilityFeeRequest request) {
        Ho ho = hoRepository.findById(request.getHoId())
                .orElseThrow(() -> new IllegalArgumentException("Ho not found"));

        MeterReading meterReading = MeterReading.builder()
                .ho(ho)
                .billingMonth(request.getBillingMonth())
                .billUuid(UUID.randomUUID())
                .createdAt(LocalDateTime.now())
                .build();

        request.getItems().forEach(req -> {
            UtilityFee fee = UtilityFee.builder()
                    .meterReading(meterReading)
                    .name(req.getName())
                    .price(req.getPrice())
                    .category(req.getCategory())
                    .createdAt(LocalDateTime.now())
                    .build();

            meterReading.addUtilityFee(fee);
        });

        MeterReading saved = meterReadingRepository.save(meterReading);
        return toResponse(saved);
    }

    // =============================
    // 관리자: 단지별 공과금 조회
    // =============================
    @Transactional(readOnly = true)
    public List<UtilityFeeResponse> getCurrentUtilityFeesByApartment(Long apartmentId) {
        return meterReadingRepository.findByHo_Apartment_Id(apartmentId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // =============================
    // 사용자: 세대별 공과금 조회
    // =============================
    @Transactional(readOnly = true)
    public List<UtilityFeeResponse> getCurrentUtilityFeesByHo(Long hoId) {
        return meterReadingRepository.findByHo_Id(hoId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // =============================
    // 관리자: 공과금 상세
    // =============================
    @Transactional(readOnly = true)
    public UtilityFeeResponse getUtilityFeeForAdmin(Long utilityFeeId, Long apartmentId) {
        MeterReading meterReading =
                meterReadingRepository.findByIdAndHo_Apartment_Id(utilityFeeId, apartmentId)
                        .orElseThrow(() -> new IllegalArgumentException("UtilityFee not found"));

        return toResponse(meterReading);
    }

    // =============================
    // 사용자: 공과금 상세
    // =============================
    @Transactional(readOnly = true)
    public UtilityFeeResponse getUtilityFeeForMember(Long utilityFeeId, Long hoId) {
        MeterReading meterReading =
                meterReadingRepository.findByIdAndHo_Id(utilityFeeId, hoId)
                        .orElseThrow(() -> new IllegalArgumentException("UtilityFee not found"));

        return toResponse(meterReading);
    }

    // =============================
    // Entity → DTO
    // =============================
    private UtilityFeeResponse toResponse(MeterReading meterReading) {
        return UtilityFeeResponse.builder()
                .id(meterReading.getId())
                .hoId(meterReading.getHo().getId())
                .billingMonth(meterReading.getBillingMonth())
                .totalAmount(
                        meterReading.getItems().stream()
                                .mapToInt(UtilityFee::getPrice)
                                .sum()
                )
                .items(
                        meterReading.getItems().stream()
                                .map(item -> UtilityFeeResponse.Item.builder()
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
