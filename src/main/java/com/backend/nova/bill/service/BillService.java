package com.backend.nova.bill.service;

import com.backend.nova.apartment.entity.Ho;
import com.backend.nova.apartment.repository.HoRepository;
import com.backend.nova.bill.dto.*;
import com.backend.nova.bill.entity.*;
import com.backend.nova.bill.repository.BillRepository;
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
public class BillService {

    private final BillRepository billRepository;
    private final HoRepository hoRepository;

    // =============================
    // 테스트용 고지서 생성 (운영 제거)
    // =============================
    public BillResponse createBill(BillRequest request) {
        Ho ho = hoRepository.findById(request.getHoId())
                .orElseThrow(() -> new IllegalArgumentException("Ho not found"));

        Bill bill = Bill.builder()
                .ho(ho)
                .month(request.getMonth())
                .billUuid(UUID.randomUUID())
                .status(BillStatus.DRAFT)
                .createdAt(LocalDateTime.now())
                .build();

        // BillItem 생성 → 반드시 addItem 사용
        request.getItems().forEach(req -> {
            BillItem item = BillItem.builder()
                    .itemType(req.getItemType())     // UTILITY / MAINTENANCE / COMMUNITY
                    .referenceId(req.getReferenceId())
                    .name(req.getName())
                    .price(req.getPrice())
                    .build();

            bill.addItem(item);
        });

        Bill savedBill = billRepository.save(bill);
        return toResponse(savedBill);
    }

    // =============================
    // 관리자: 단지별 전체 고지서 조회
    // =============================
    @Transactional(readOnly = true)
    public List<BillResponse> getBillsByApartment(Long apartmentId) {
        return billRepository.findByHo_Apartment_Id(apartmentId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // =============================
    // 사용자: 세대별 고지서 조회
    // =============================
    @Transactional(readOnly = true)
    public List<BillResponse> getBillsByHo(Long hoId) {
        return billRepository.findByHo_Id(hoId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // =============================
    // 관리자: 단지 내 고지서 상세
    // =============================
    @Transactional(readOnly = true)
    public BillResponse getBillForAdmin(Long billId, Long apartmentId) {
        Bill bill = billRepository.findByIdAndHo_Apartment_Id(billId, apartmentId)
                .orElseThrow(() -> new IllegalArgumentException("Bill not found or not in your apartment"));

        return toResponse(bill);
    }

    // =============================
    // 사용자: 자기 세대 고지서 상세
    // =============================
    @Transactional(readOnly = true)
    public BillResponse getBillForMember(Long billId, Long hoId) {
        Bill bill = billRepository.findByIdAndHo_Id(billId, hoId)
                .orElseThrow(() -> new IllegalArgumentException("Bill not found or not in your household"));

        return toResponse(bill);
    }

    // =============================
    // Entity → DTO 변환
    // =============================
    private BillResponse toResponse(Bill bill) {
        return BillResponse.builder()
                .id(bill.getId())
                .hoId(bill.getHo().getId())
                .month(bill.getMonth())
                .totalPrice(bill.getTotalPrice())
                .status(bill.getStatus())
                .items(
                        bill.getItems().stream()
                                .map(item -> BillItemResponse.builder()
                                        .id(item.getId())
                                        .itemType(item.getItemType())
                                        .name(item.getName())
                                        .price(item.getPrice())
                                        .build())
                                .collect(Collectors.toList())
                )
                .build();
    }
}
