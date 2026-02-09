package com.backend.nova.bill.service;

import com.backend.nova.apartment.entity.Ho;
import com.backend.nova.apartment.repository.HoRepository;
import com.backend.nova.auth.admin.AdminDetails;
import com.backend.nova.bill.dto.*;
import com.backend.nova.bill.entity.Bill;
import com.backend.nova.bill.entity.BillItem;
import com.backend.nova.bill.entity.BillStatus;
import com.backend.nova.bill.repository.BillRepository;
//import com.backend.nova.meter.entity.UtilityFee;
//import com.backend.nova.meter.repository.UtilityFeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class BillGenerationService {

    private final HoRepository hoRepository;
    //private final MeterFeeRepository meterFeeRepository;
    private final BillRepository billRepository;




    // =============================
// 고지서 발행 (관리자 전용)
// =============================
    public BillResponse issueBill(
            BillReadyRequest request,
            Authentication authentication
    ) {
        // 1. 관리자 권한 체크
        Object principal = authentication.getPrincipal();
        if (!(principal instanceof AdminDetails admin)) {
            throw new AccessDeniedException("고지서 발행 권한이 없습니다.");
        }

        String month = request.getMonth(); // "2025-02"
        Long hoId = request.getHoId();

        // 2. 사전 중복 체크 (친절한 에러)
        if (billRepository.existsByHo_IdAndMonth(hoId, month)) {
            throw new IllegalStateException(
                    "이미 해당 월의 고지서가 발행되어 있습니다. (month=" + month + ")"
            );
        }

        // 3. Ho 조회 (아파트 권한 체크 포함)
        Ho ho = hoRepository.findByIdAndDong_Apartment_Id(
                        hoId, admin.getApartmentId()
                )
                .orElseThrow(() -> new IllegalArgumentException("해당 세대를 찾을 수 없습니다."));

        // 4. 고지서 생성
        Bill bill = Bill.builder()
                .ho(ho)
                .billUid(UUID.randomUUID().toString())
                .month(month)
                .status(BillStatus.READY)
                .createdAt(LocalDateTime.now())
                .issuedAt(LocalDateTime.now())
                .build();

        Bill savedBill = billRepository.save(bill);

        return toResponse(savedBill);
    }

    // =============================
    // 월별 고지서 일괄 발행 (아파트 전체)
    // =============================
    public BillBulkReadyResponse issueBillsForApartment(
            BillBulkReadyRequest request,
            Authentication authentication
    ) {
        Object principal = authentication.getPrincipal();
        if (!(principal instanceof AdminDetails admin)) {
            throw new AccessDeniedException("고지서 발행 권한이 없습니다.");
        }

        String month = request.getMonth(); // YYYY-MM
        Long apartmentId = admin.getApartmentId();

        // 1. 아파트 내 모든 세대 조회
        List<Ho> hos = hoRepository.findByDong_Apartment_Id(apartmentId);

        int total = hos.size();
        int issued = 0;
        int skipped = 0;

        for (Ho ho : hos) {

            // 2. 이미 발행된 세대는 스킵
            if (billRepository.existsByHo_IdAndMonth(ho.getId(), month)) {
                skipped++;
                continue;
            }

            try {
                Bill bill = Bill.builder()
                        .ho(ho)
                        .billUid(UUID.randomUUID().toString())
                        .month(month)
                        .status(BillStatus.READY)
                        .createdAt(LocalDateTime.now())
                        .issuedAt(LocalDateTime.now())
                        .build();

                billRepository.save(bill);
                issued++;

            } catch (DataIntegrityViolationException e) {
                // 동시성 / 레이스 상황 대비 (DB unique)
                skipped++;
            }
        }

        return BillBulkReadyResponse.builder()
                .month(month)
                .totalHoCount(total)
                .issuedCount(issued)
                .skippedCount(skipped)
                .build();
    }



    // =============================
    // 테스트용 고지서 생성 (운영 제거)
    // =============================
    public BillResponse createBill(BillRequest request) {
        Ho ho = hoRepository.findById(request.getHoId())
                .orElseThrow(() -> new IllegalArgumentException("Ho not found"));

        Bill bill = Bill.builder()
                .ho(ho)
                .month(request.getMonth())
                .billUid("BILL-" + UUID.randomUUID())
                .status(BillStatus.READY)
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
    // 미터기 고지서 생성
    // =============================
//    public void generateMeterBills(Long apartmentId, YearMonth month) {
//
//        // 1. 이미 고지서 생성됐는지 체크
//        if (billRepository.existsByHo_Apartment_IdAndBillingMonth(apartmentId, month)) {
//            throw new IllegalStateException("이미 고지서가 생성된 월입니다.");
//        }
//
//        // 2. 단지 내 세대 조회
//        List<Ho> hos = hoRepository.findByApartment_Id(apartmentId);
//
//        for (Ho ho : hos) {
//
//            // 3. 해당 세대의 월 공과금 조회
//            List<MeterFee> fees =
//                    meterFeeRepository.findByHo_IdAndMonth(
//                            ho.getId(),
//                            month
//                    );
//
//            if (fees.isEmpty()) {
//                continue; // 공과금 없으면 고지서 생성 안 함
//            }
//
//            // 4. Bill 생성
//            Bill bill = Bill.builder()
//                    .ho(ho)
//                    .month(month)
//                    .billUuid(UUID.randomUUID())
//                    .status(BillStatus.DRAFT) // 미납
//                    .createdAt(LocalDateTime.now())
//                    .build();
//
//            // 5. MeterFee → BillItem 변환
//            int totalAmount = 0;
//
//            for (MeterFee fee : fees) {
//                BillItem item = BillItem.builder()
//                        .bill(bill)
//                        .itemType(fee.getMeterType().name()) // WATER / ELECTRIC / GAS
//                        .title(getTitle(fee))
//                        .price(fee.getCalculatedFee())
//                        .referenceId(fee.getId()) // 추적용
//                        .build();
//
//                bill.addItem(item);
//                totalAmount += fee.getCalculatedFee();
//            }
//
//            // 6. 총액 세팅
//            bill.updateTotalPrice(totalAmount);
//
//            billRepository.save(bill);
//        }
//    }

    // =============================
    // 미터기 고지서 항목명 생성
    // =============================
//    private String getTitle(MeterFee fee) {
//        return switch (fee.getMeterType()) {
//            case WATER -> "수도 요금";
//            case ELECTRIC -> "전기 요금";
//            case GAS -> "가스 요금";
//        };
//    }


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
