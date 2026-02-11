package com.backend.nova.bill.service;

import com.backend.nova.apartment.entity.Ho;
import com.backend.nova.apartment.repository.HoRepository;
import com.backend.nova.bill.entity.Bill;
import com.backend.nova.bill.entity.BillItem;
import com.backend.nova.bill.entity.BillItemType;
import com.backend.nova.bill.entity.BillStatus;
import com.backend.nova.bill.repository.BillRepository;
import com.backend.nova.management.entity.ManagementFee;
import com.backend.nova.management.repository.ManagementFeeRepository;
//import com.backend.nova.meter.entity.MeterFee;
//import com.backend.nova.meter.repository.MeterFeeRepository;
import com.backend.nova.reservation.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class BillGenerationService {

    private final HoRepository hoRepository;
    private final BillRepository billRepository;
    private final ManagementFeeRepository managementFeeRepository;
    private final ReservationRepository reservationRepository;
    //private final UtilityFeeRepository utilityFeeRepository;


    // =============================
    // 월별 고지서 일괄 생성
    // =============================
    public void generateBills(Long apartmentId, String month) {

        YearMonth yearMonth;
        try {
            yearMonth = YearMonth.parse(month);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("month는 YYYY-MM 형식이어야 합니다.");
        }

        String billingMonth = yearMonth.toString(); // YearMonth → "YYYY-MM"

        // 1. 중복 생성 방지
        if (billRepository.existsByHo_Dong_Apartment_IdAndBillMonth(apartmentId, billingMonth)) {
            throw new IllegalStateException("이미 고지서가 생성된 월입니다.");
        }

        // 2. 단지 내 세대 조회
        List<Ho> hos = hoRepository.findByDong_Apartment_Id(apartmentId);

        // 3. 단지 관리비 항목 조회 (활성만)
        List<ManagementFee> managementFees =
                managementFeeRepository.findByApartment_IdAndActiveTrue(apartmentId);

        // 고지서 생성시 월 범위
        LocalDateTime startOfMonth = yearMonth.atDay(1).atStartOfDay();
        LocalDateTime endOfMonth = yearMonth.atEndOfMonth().atTime(23, 59, 59);

        for (Ho ho : hos) {

            // 4. Bill 생성
            Bill bill = Bill.builder()
                    .ho(ho)
                    .billMonth(yearMonth.toString()) // YYYY-MM
                    .billUid("BILL-" + UUID.randomUUID())
                    .status(BillStatus.OPEN)
                    .build();

            boolean hasAnyItem = false;

            // =============================
            // 5. 관리비 → BillItem
            // =============================
            for (ManagementFee fee : managementFees) {
                BillItem item = BillItem.builder()
                        .itemType(BillItemType.MANAGEMENT)
                        .referenceId(fee.getId())
                        .name(fee.getName())
                        .price(fee.getPrice())
                        .build();

                bill.addItem(item);
                hasAnyItem = true;
            }

            // =============================
            // 5. 커뮤니티 사용료(예약) → BillItem
            // =============================
//            List<Reservation> reservations =
//                    reservationRepository.findByMember_Ho_IdAndPaymentMethodAndStatusAndStartTimeBetween(
//                            ho.getId(),
//                            PaymentMethod.MANAGEMENT_FEE,
//                            Status.CONFIRMED,
//                            startOfMonth,
//                            endOfMonth
//                    );
//
//            for (Reservation reservation : reservations) {
//                BillItem item = BillItem.builder()
//                        .itemType(BillItemType.COMMUNITY)
//                        .referenceId(reservation.getId())
//                        .name("커뮤니티 시설 이용료")
//                        .price(BigDecimal.valueOf(reservation.getTotalPrice()))
//                        .build();
//
//                bill.addItem(item);
//                hasAnyItem = true;
//            }

            // =============================
            // 6. 공과금 → BillItem
            // =============================
//            List<UtilityFee> utilityFees =
//                    utilityFeeRepository.findByHo_IdAndMonth(ho.getId(), month);
//
//            for (UtilityFee fee : utilityFees) {
//                BillItem item = BillItem.builder()
//                        .itemType(BillItemType.UTILITY)
//                        .referenceId(fee.getId())
//                        .name(getUtilityTitle(fee))
//                        .price(fee.getCalculatedFee())
//                        .build();
//
//                bill.addItem(item);
//                hasAnyItem = true;
//            }

            // 7. 항목 하나도 없으면 고지서 생성 안 함
            if (!hasAnyItem) {
                continue;
            }

            //고지서 저장
            billRepository.save(bill);
        }
    }

    // =============================
    // 공과금 항목명
    // =============================
//    private String getUtilityTitle(UtilityFee fee) {
//        return switch (fee.getMeterType()) {
//            case WATER -> "수도 요금";
//            case ELECTRIC -> "전기 요금";
//            case GAS -> "가스 요금";
//        };
//    }
}

