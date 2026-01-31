package com.backend.nova.apartmentbilllog.service;

import com.backend.nova.apartment.entity.Ho;
import com.backend.nova.apartment.repository.HoRepository;
import com.backend.nova.apartmentbilllog.dto.*;
import com.backend.nova.apartmentbilllog.entity.*;
import com.backend.nova.apartmentbilllog.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BillService {

    private final BillRepository billRepository;
    private final BillItemRepository billItemRepository;
    private final HoRepository hoRepository; // Ho 엔티티 조회용

    // ===== 테스트용 생성 API (운영에서는 제거 예정) =====
    public BillResponse createBill(BillRequest request) {
        Ho ho = hoRepository.findById(request.getHoId())
                .orElseThrow(() -> new IllegalArgumentException("Ho not found"));

        Bill bill = Bill.builder()
                .ho(ho) // 엔티티 연관관계로 세팅
                .billingMonth(request.getBillingMonth())
                .totalAmount(request.getTotalAmount())
                .billUuid(UUID.randomUUID())
                .status(false) // 기본은 미납
                .createdAt(LocalDateTime.now())
                .build();

        List<BillItem> items = request.getItems().stream()
                .map(req -> BillItem.builder()
                        .bill(bill)
                        .name(req.getName())
                        .price(req.getPrice())
                        .category(req.getCategory())
                        .createdAt(LocalDateTime.now())
                        .build())
                .collect(Collectors.toList());

        bill.getItems().addAll(items); // setItems 대신 getItems().addAll()
        billRepository.save(bill);

        return toResponse(bill);
    }

    // ===== 관리자: 단지별 전체 고지서 조회 =====
    public List<BillResponse> getBillsByApartment(Long apartmentId) {
        return billRepository.findByHo_Apartment_Id(apartmentId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // ===== 사용자: 세대별 고지서 조회 =====
    public List<BillResponse> getBillsByHo(Long hoId) {
        return billRepository.findByHo_Id(hoId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // ===== 관리자: 단지 내 특정 고지서 상세 조회 =====
    public BillResponse getBillForAdmin(Long billId, Long apartmentId) {
        Bill bill = billRepository.findByIdAndHo_Apartment_Id(billId, apartmentId)
                .orElseThrow(() -> new IllegalArgumentException("Bill not found or not in your apartment"));
        return toResponse(bill);
    }

    // ===== 사용자: 자기 세대 고지서 상세 조회 =====
    public BillResponse getBillForMember(Long billId, Long hoId) {
        Bill bill = billRepository.findByIdAndHo_Id(billId, hoId)
                .orElseThrow(() -> new IllegalArgumentException("Bill not found or not in your household"));
        return toResponse(bill);
    }

    // ===== 변환 로직 (Entity → Response) =====
    private BillResponse toResponse(Bill bill) {
        return BillResponse.builder()
                .id(bill.getId())
                .hoId(bill.getHo().getId()) // 엔티티에서 꺼냄
                .billingMonth(bill.getBillingMonth())
                .totalAmount(bill.getTotalAmount())
                .status(bill.isStatus())
                .items(bill.getItems().stream()
                        .map(item -> BillItemResponse.builder()
                                .id(item.getId())
                                .name(item.getName())
                                .price(item.getPrice())
                                .category(item.getCategory())
                                .build())
                        .collect(Collectors.toList()))
                .build();
    }
}