package com.backend.nova.bill.service;

import com.backend.nova.apartment.entity.Ho;
import com.backend.nova.apartment.repository.HoRepository;
import com.backend.nova.bill.dto.BillItemResponse;
import com.backend.nova.bill.dto.BillRequest;
import com.backend.nova.bill.dto.BillResponse;
import com.backend.nova.bill.entity.Bill;
import com.backend.nova.bill.entity.BillItem;
import com.backend.nova.bill.entity.BillStatus;
import com.backend.nova.bill.repository.BillRepository;
//import com.backend.nova.meter.entity.UtilityFee;
//import com.backend.nova.meter.repository.UtilityFeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
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
    // 테스트용 고지서 생성 (운영 제거)
    // =============================
    public BillResponse createBill(BillRequest request) {
        Ho ho = hoRepository.findById(request.getHoId())
                .orElseThrow(() -> new IllegalArgumentException("Ho not found"));

        Bill bill = Bill.builder()
                .ho(ho)
                .month(request.getMonth())
                .billUuid(UUID.randomUUID())
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
