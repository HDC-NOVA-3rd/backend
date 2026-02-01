package com.backend.nova.bill.service;

import com.backend.nova.apartment.entity.Ho;
import com.backend.nova.apartment.repository.HoRepository;
import com.backend.nova.bill.entity.Bill;
import com.backend.nova.bill.entity.BillItem;
import com.backend.nova.bill.entity.BillStatus;
import com.backend.nova.bill.repository.BillRepository;
import com.backend.nova.utilityfee.entity.UtilityFee;
import com.backend.nova.utilityfee.repository.UtilityFeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class BillGenerationService {

    private final HoRepository hoRepository;
    private final UtilityFeeRepository utilityFeeRepository;
    private final BillRepository billRepository;

    // =============================
    // 고지서 생성
    // =============================
    public void generateBills(Long apartmentId, YearMonth month) {

        // 1. 이미 고지서 생성됐는지 체크
        if (billRepository.existsByHo_Apartment_IdAndBillingMonth(apartmentId, month)) {
            throw new IllegalStateException("이미 고지서가 생성된 월입니다.");
        }

        // 2. 단지 내 세대 조회
        List<Ho> hos = hoRepository.findByApartment_Id(apartmentId);

        for (Ho ho : hos) {

            // 3. 해당 세대의 월 공과금 조회
            List<UtilityFee> fees =
                    utilityFeeRepository.findByHo_IdAndMonth(
                            ho.getId(),
                            month
                    );

            if (fees.isEmpty()) {
                continue; // 공과금 없으면 고지서 생성 안 함
            }

            // 4. Bill 생성
            Bill bill = Bill.builder()
                    .ho(ho)
                    .month(month)
                    .billUuid(UUID.randomUUID())
                    .status(BillStatus.DRAFT) // 미납
                    .createdAt(LocalDateTime.now())
                    .build();

            // 5. UtilityFee → BillItem 변환
            int totalAmount = 0;

            for (UtilityFee fee : fees) {
                BillItem item = BillItem.builder()
                        .bill(bill)
                        .itemType(fee.getMeterType().name()) // WATER / ELECTRIC / GAS
                        .title(getTitle(fee))
                        .price(fee.getCalculatedFee())
                        .referenceId(fee.getId()) // 추적용
                        .build();

                bill.addItem(item);
                totalAmount += fee.getCalculatedFee();
            }

            // 6. 총액 세팅
            bill.updateTotalPrice(totalAmount);

            billRepository.save(bill);
        }
    }

    // =============================
    // 항목명 생성
    // =============================
    private String getTitle(UtilityFee fee) {
        return switch (fee.getMeterType()) {
            case WATER -> "수도 요금";
            case ELECTRIC -> "전기 요금";
            case GAS -> "가스 요금";
        };
    }
}
