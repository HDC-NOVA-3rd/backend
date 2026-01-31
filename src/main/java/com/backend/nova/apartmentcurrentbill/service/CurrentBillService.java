package com.backend.nova.apartmentcurrentbill.service;

import com.backend.nova.apartment.entity.Ho;
import com.backend.nova.apartment.repository.HoRepository;
import com.backend.nova.apartmentcurrentbill.dto.*;
import com.backend.nova.apartmentcurrentbill.entity.*;
import com.backend.nova.apartmentcurrentbill.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CurrentBillService {

    private final CurrentBillRepository currentBillRepository;
    private final CurrentBillItemRepository currentBillItemRepository;
    private final HoRepository hoRepository;

    // ===== 테스트용 생성 API (운영에서는 제거 예정) =====
    public CurrentBillResponse createCurrentBill(CurrentBillRequest request) {
        Ho ho = hoRepository.findById(request.getHoId())
                .orElseThrow(() -> new IllegalArgumentException("Ho not found"));

        CurrentBill currentBill = CurrentBill.builder()
                .ho(ho) // 엔티티 연관관계로 세팅
                .billingMonth(request.getBillingMonth())
                .totalAmount(request.getTotalAmount())
                .billUuid(UUID.randomUUID())
                .createdAt(LocalDateTime.now())
                .build();

        List<CurrentBillItem> items = request.getItems().stream()
                .map(req -> CurrentBillItem.builder()
                        .currentBill(currentBill)
                        .name(req.getName())
                        .price(req.getPrice())
                        .category(req.getCategory())
                        .createdAt(LocalDateTime.now())
                        .build())
                .collect(Collectors.toList());

        currentBill.getItems().addAll(items);
        currentBillRepository.save(currentBill);

        return toResponse(currentBill);
    }

    // ===== 관리자: 단지별 현재 고지서 조회 =====
    public List<CurrentBillResponse> getCurrentBillsByApartment(Long apartmentId) {
        return currentBillRepository.findByHo_Apartment_Id(apartmentId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // ===== 사용자: 세대별 현재 고지서 조회 =====
    public List<CurrentBillResponse> getCurrentBillsByHo(Long hoId) {
        return currentBillRepository.findByHo_Id(hoId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // ===== 관리자: 단지 내 특정 현재 고지서 상세 조회 =====
    public CurrentBillResponse getCurrentBillForAdmin(Long currentBillId, Long apartmentId) {
        CurrentBill currentBill = currentBillRepository.findByIdAndHo_Apartment_Id(currentBillId, apartmentId)
                .orElseThrow(() -> new IllegalArgumentException("CurrentBill not found or not in your apartment"));
        return toResponse(currentBill);
    }

    // ===== 사용자: 자기 세대 현재 고지서 상세 조회 =====
    public CurrentBillResponse getCurrentBillForMember(Long currentBillId, Long hoId) {
        CurrentBill currentBill = currentBillRepository.findByIdAndHo_Id(currentBillId, hoId)
                .orElseThrow(() -> new IllegalArgumentException("CurrentBill not found or not in your household"));
        return toResponse(currentBill);
    }

    // ===== 변환 로직 (Entity → Response) =====
    private CurrentBillResponse toResponse(CurrentBill currentBill) {
        return CurrentBillResponse.builder()
                .id(currentBill.getId())
                .hoId(currentBill.getHo().getId()) // 엔티티에서 꺼냄
                .billingMonth(currentBill.getBillingMonth())
                .totalAmount(currentBill.getTotalAmount())
                .items(currentBill.getItems().stream()
                        .map(item -> CurrentBillItemResponse.builder()
                                .id(item.getId())
                                .name(item.getName())
                                .price(item.getPrice())
                                .category(item.getCategory())
                                .build())
                        .collect(Collectors.toList()))
                .build();
    }
}