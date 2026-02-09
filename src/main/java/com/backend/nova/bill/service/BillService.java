package com.backend.nova.bill.service;

import com.backend.nova.auth.admin.AdminDetails;
import com.backend.nova.auth.member.MemberDetails;
import com.backend.nova.bill.dto.*;
import com.backend.nova.bill.entity.Bill;
import com.backend.nova.bill.repository.BillRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class BillService {

    private final BillRepository billRepository;

    // =============================
    // 고지서 목록
    // =============================
    @Transactional(readOnly = true)
    public List<BillResponse> getBills(Authentication authentication) {
        Object principal = authentication.getPrincipal();

        if (principal instanceof AdminDetails admin) {
            return getBillsByApartment(admin.getApartmentId());
        }

        if (principal instanceof MemberDetails member) {
            return getBillsByHo(member.getHoId());
        }

        throw new AccessDeniedException("고지서 조회 권한이 없습니다.");
    }

    // =============================
    // 고지서 상세
    // =============================
    @Transactional(readOnly = true)
    public BillResponse getBill(Long billId, Authentication authentication) {
        Object principal = authentication.getPrincipal();

        if (principal instanceof AdminDetails admin) {
            return getBillForAdmin(billId, admin.getApartmentId());
        }

        if (principal instanceof MemberDetails member) {
            return getBillForMember(billId, member.getHoId());
        }

        throw new AccessDeniedException("고지서 조회 권한이 없습니다.");
    }

    // =============================
    // 관리자 전용
    // =============================
    private List<BillResponse> getBillsByApartment(Long apartmentId) {
        return billRepository.findByHo_Dong_Apartment_Id(apartmentId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private BillResponse getBillForAdmin(Long billId, Long apartmentId) {
        Bill bill = billRepository.findByIdAndHo_Dong_Apartment_Id(billId, apartmentId)
                .orElseThrow(() -> new IllegalArgumentException("해당 고지서를 조회할 수 없습니다."));
        return toResponse(bill);
    }

    // =============================
    // 사용자 전용
    // =============================
    private List<BillResponse> getBillsByHo(Long hoId) {
        return billRepository.findByHo_Id(hoId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private BillResponse getBillForMember(Long billId, Long hoId) {
        Bill bill = billRepository.findByIdAndHo_Id(billId, hoId)
                .orElseThrow(() -> new IllegalArgumentException("해당 고지서를 조회할 수 없습니다."));
        return toResponse(bill);
    }

    // =============================
    // Entity → DTO
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

