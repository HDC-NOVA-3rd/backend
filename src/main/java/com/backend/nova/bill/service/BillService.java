package com.backend.nova.bill.service;

import com.backend.nova.bill.dto.*;
import com.backend.nova.bill.entity.*;
import com.backend.nova.bill.repository.BillRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class BillService {

    private final BillRepository billRepository;

    // =============================
    // 관리자: 단지별 전체 고지서 조회
    // =============================
    public List<BillSummaryResponse> getBillsByApartment(Long apartmentId) {
        return billRepository.findSummaryByApartmentId(apartmentId);
    }

    // =============================
    // 사용자: 세대별 고지서 조회
    // =============================
    public List<BillSummaryResponse> getBillsByHo(Long hoId) {
        return billRepository.findSummaryByHoId(hoId);
    }

    // =============================
    // 관리자: 단지 내 고지서 상세
    // =============================
    public BillDetailResponse getBillForAdmin(Long billId, Long apartmentId) {
        Bill bill = billRepository.findDetailForAdmin(billId, apartmentId)
                .orElseThrow(() -> new IllegalArgumentException("고지서를 찾을 수 없거나 권한이 없습니다."));
        return convertToDetailResponse(bill);
    }

    // =============================
    // 사용자: 자기 세대 고지서 상세
    // =============================
    public BillDetailResponse getBillForMember(Long billId, Long hoId) {
        Bill bill = billRepository.findDetailForMember(billId, hoId)
                .orElseThrow(() -> new IllegalArgumentException("고지서를 찾을 수 없거나 권한이 없습니다."));
        return convertToDetailResponse(bill);
    }

    // =============================
    // Entity → DTO 변환
    // =============================
    private BillDetailResponse convertToDetailResponse(Bill bill) {
        return BillDetailResponse.builder()
                .billId(bill.getId())
                .apartmentName(bill.getHo().getDong().getApartment().getName())
                .dongName(bill.getHo().getDong().getDongNo())
                .hoName(bill.getHo().getHoNo())
                .billMonth(bill.getBillMonth())
                .totalPrice(bill.getTotalPrice())
                .status(bill.getStatus())
                .dueDate(bill.getDueDate())
                .items(bill.getItems().stream()
                        .map(item -> BillItemResponse.builder()
                                .id(item.getId())
                                .name(item.getName())
                                .price(item.getPrice())
                                .itemType(item.getItemType())
                                .build())
                        .toList())
                .build();
    }
}
