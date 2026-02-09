package com.backend.nova.bill.service;

import com.backend.nova.auth.admin.AdminDetails;
import com.backend.nova.auth.member.MemberDetails;
import com.backend.nova.bill.dto.*;
import com.backend.nova.bill.entity.Bill;
import com.backend.nova.bill.entity.BillStatus;
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

    @Service
    @RequiredArgsConstructor
    @Transactional(readOnly = true)
    public class BillService {

        private final BillRepository billRepository;

        public List<BillResponse> getBillsByApartment(Long apartmentId) {
            return billRepository.findByHo_Dong_Apartment_Id(apartmentId)
                    .stream().map(this::toResponse).toList();
        }

        public List<BillResponse> getBillsByHo(Long hoId) {
            return billRepository.findByHo_Id(hoId)
                    .stream().map(this::toResponse).toList();
        }

        public BillResponse getBillForAdmin(Long billId, Long apartmentId) {
            Bill bill = billRepository.findByIdAndHo_Dong_Apartment_Id(billId, apartmentId)
                    .orElseThrow(() -> new BusinessException(BILL_NOT_FOUND_OR_NO_PERMISSION));
            return toResponse(bill);
        }

        public BillResponse getBillForMember(Long billId, Long hoId) {
            Bill bill = billRepository.findByIdAndHo_Id(billId, hoId)
                    .orElseThrow(() -> new BusinessException(BILL_NOT_FOUND_OR_NO_PERMISSION));
            return toResponse(bill);
        }

        public List<BillResponse> getUnpaidBillsByApartment(Long apartmentId) {
            return billRepository.findByHo_Dong_Apartment_IdAndStatus(apartmentId, BillStatus.READY)
                    .stream().map(this::toResponse).toList();
        }

        public List<BillResponse> getUnpaidBillsByHo(Long hoId) {
            return billRepository.findByHo_IdAndStatus(hoId, BillStatus.READY)
                    .stream().map(this::toResponse).toList();
        }

        public List<BillResponse> getUnpaidBillsByMonth(String month, Long apartmentId) {
            return billRepository.findByHo_Dong_Apartment_IdAndMonthAndStatus(
                            apartmentId, month, BillStatus.READY)
                    .stream().map(this::toResponse).toList();
        }

        public List<BillResponse> getConfirmedOrReadyBills(Long hoId) {
            return billRepository.findByHoIdAndStatusIn(
                            hoId,
                            List.of(BillStatus.OPEN, BillStatus.READY, BillStatus.PAID))
                    .stream().map(this::toResponse).toList();
        }

        // toResponse() 생략 (기존과 동일)
    }

    //YearMonth ym = YearMonth.now();
    //String month = ym.toString(); // "2025-02"

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


    //세대별 미납조회 (입주민 화면)

    //아파트 전체 미납조회 (관리자)

    //특정 월 미납
    @Transactional(readOnly = true)
    public List<BillResponse> getUnpaidBillsByMonth(
            String month,
            Authentication authentication
    ) {
        AdminDetails admin = (AdminDetails) authentication.getPrincipal();

        return billRepository
                .findByHo_Dong_Apartment_IdAndMonthAndStatus(
                        admin.getApartmentId(),
                        month,
                        BillStatus.READY
                )
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }


    // =============================
// 미납 고지서 조회
// =============================
    @Transactional(readOnly = true)
    public List<BillResponse> getUnpaidBills(Authentication authentication) {

        Object principal = authentication.getPrincipal();

        // 관리자: 아파트 전체 미납
        if (principal instanceof AdminDetails admin) {
            return billRepository
                    .findByHo_Dong_Apartment_IdAndStatus(
                            admin.getApartmentId(),
                            BillStatus.READY
                    )
                    .stream()
                    .map(this::toResponse)
                    .collect(Collectors.toList());
        }

        // 입주민: 본인 세대 미납
        if (principal instanceof MemberDetails member) {
            return billRepository
                    .findByHo_IdAndStatus(
                            member.getHoId(),
                            BillStatus.READY
                    )
                    .stream()
                    .map(this::toResponse)
                    .collect(Collectors.toList());
        }

        throw new AccessDeniedException("미납 고지서 조회 권한이 없습니다.");
    }

    //입주민 확정전 고지서 상시 조회
    private List<BillResponse> getBillsByHo(Long hoId) {
        return billRepository
                .findByHo_IdAndStatusIn(
                        hoId,
                        List.of(
                                BillStatus.OPEN,
                                BillStatus.READY,
                                BillStatus.PAID
                        )
                )
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    package com.backend.nova.bill.service;

import com.backend.nova.auth.admin.AdminDetails;
import com.backend.nova.bill.dto.*;
import com.backend.nova.bill.entity.Bill;
import com.backend.nova.bill.entity.BillStatus;
import com.backend.nova.bill.repository.BillRepository;
import com.backend.nova.common.exception.BusinessException;
import com.backend.nova.common.security.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

import static com.backend.nova.common.exception.ErrorCode.*;

    @Service
    @RequiredArgsConstructor
    @Transactional(readOnly = true)
    public class BillService {

        private final BillRepository billRepository;

        // 현재 로그인 사용자 기준 고지서 전체 목록
        public List<BillResponse> getMyBills() {
            if (SecurityUtil.isAdmin()) {
                Long apartmentId = SecurityUtil.getCurrentAdminApartmentId();
                return toResponses(billRepository.findByHoDongApartmentId(apartmentId));
            }

            if (SecurityUtil.isMember()) {
                Long hoId = SecurityUtil.getCurrentMemberHoId();
                return toResponses(billRepository.findByHoId(hoId));
            }

            throw new BusinessException(FORBIDDEN, "고지서 조회 권한이 없습니다.");
        }

        public BillResponse getBill(Long billId) {
            if (SecurityUtil.isAdmin()) {
                Long apartmentId = SecurityUtil.getCurrentAdminApartmentId();
                Bill bill = billRepository.findByIdAndHoDongApartmentId(billId, apartmentId)
                        .orElseThrow(() -> new BusinessException(BILL_NOT_FOUND));
                return toResponse(bill);
            }

            if (SecurityUtil.isMember()) {
                Long hoId = SecurityUtil.getCurrentMemberHoId();
                Bill bill = billRepository.findByIdAndHoId(billId, hoId)
                        .orElseThrow(() -> new BusinessException(BILL_NOT_FOUND_OR_NO_PERMISSION));
                return toResponse(bill);
            }

            throw new BusinessException(FORBIDDEN, "고지서 조회 권한이 없습니다.");
        }

        // 미납 고지서 (입주민 → 본인 / 관리자 → 전체)
        public List<BillResponse> getUnpaidBills() {
            if (SecurityUtil.isAdmin()) {
                Long apartmentId = SecurityUtil.getCurrentAdminApartmentId();
                return toResponses(
                        billRepository.findByHoDongApartmentIdAndStatus(apartmentId, BillStatus.READY)
                );
            }

            if (SecurityUtil.isMember()) {
                Long hoId = SecurityUtil.getCurrentMemberHoId();
                return toResponses(
                        billRepository.findByHoIdAndStatus(hoId, BillStatus.READY)
                );
            }

            throw new BusinessException(FORBIDDEN);
        }

        // 관리자 전용 - 특정 월 미납 목록
        public List<BillResponse> getUnpaidBillsByMonth(String month) {
            SecurityUtil.validateAdmin();  // 관리자 아니면 바로 예외

            Long apartmentId = SecurityUtil.getCurrentAdminApartmentId();

            return toResponses(
                    billRepository.findByHoDongApartmentIdAndMonthAndStatus(
                            apartmentId, month, BillStatus.READY)
            );
        }

        // 입주민 전용 - OPEN + READY + PAID 상태 고지서 목록
        public List<BillResponse> getConfirmedOrReadyBillsForMember() {
            SecurityUtil.validateMember();

            Long hoId = SecurityUtil.getCurrentMemberHoId();

            return toResponses(
                    billRepository.findByHoIdAndStatusIn(
                            hoId,
                            List.of(BillStatus.OPEN, BillStatus.READY, BillStatus.PAID)
                    )
            );
        }

        // -------------------------------------------------------------------------
        //          헬퍼 메서드
        // -------------------------------------------------------------------------
        private List<BillResponse> toResponses(List<Bill> bills) {
            return bills.stream().map(this::toResponse).collect(Collectors.toList());
        }

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
                                    .toList()
                    )
                    .build();
        }
    }

}

