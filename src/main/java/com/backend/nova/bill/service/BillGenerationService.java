package com.backend.nova.bill.service;

import com.backend.nova.apartment.entity.Apartment;
import com.backend.nova.apartment.entity.Ho;
import com.backend.nova.apartment.repository.ApartmentRepository;
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
import com.backend.nova.reservation.entity.PaymentMethod;
import com.backend.nova.reservation.entity.Reservation;
import com.backend.nova.reservation.entity.Status;
import com.backend.nova.reservation.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class BillGenerationService {

    private final ApartmentRepository apartmentRepository;
    private final HoRepository hoRepository;
    private final BillRepository billRepository;
    private final ManagementFeeRepository managementFeeRepository;
    private final ReservationRepository reservationRepository;

    // 시스템 공통 설정값 (단지별 컬럼 대신 전역 설정 사용)
    private static final int GLOBAL_GEN_DAY = 15; // 매월 15일 고지서 생성(OPEN)
    private static final int GLOBAL_PUB_DAY = 25; // 매월 25일 고지서 발행(READY)

    /**
     * 통합 스케줄러: 매일 자정(00:00) 실행
     * 모든 아파트 단지에 대해 동일한 날짜 규칙 적용 (생성 및 발행 체크)
     */
    @Scheduled(cron = "0 0 0 * * ?")
    public void autoBillScheduler() {
        LocalDate today = LocalDate.now();
        int dayOfMonth = today.getDayOfMonth();
        String currentMonth = YearMonth.from(today).toString(); // "YYYY-MM"

        List<Apartment> apartments = apartmentRepository.findAll();

        for (Apartment apt : apartments) {
            // 1. 생성일 체크 (매월 15일 - OPEN 상태 생성)
            if (dayOfMonth == GLOBAL_GEN_DAY) {
                generateBills(apt.getId(), currentMonth);
            }

            // 2. 발행일 체크 (매월 25일 - READY 상태로 변경 및 입주민 공개)
            if (dayOfMonth == GLOBAL_PUB_DAY) {
                publishBills(apt.getId(), currentMonth);
            }
        }
    }

    // =============================
    // 월별 고지서 일괄 생성
    // =============================
    /**
     * [단계 1] 고지서 초안 생성 (OPEN)
     * 스케줄러에 의해 자동으로 실행되거나, 관리자가 생성 버튼을 누를 때 호출
     */
    //[OPEN 단계] 기초 데이터 수집 및 저장
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
                    .billMonth(month)
                    .billUid("BILL-" + UUID.randomUUID())
                    .status(BillStatus.OPEN) // 처음엔 OPEN 상태
                    .openAt(LocalDateTime.now()) // 기록 시작 시점
                    .dueDate(LocalDate.now().plusMonths(1).withDayOfMonth(10)) // 임시 마감일
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
            // 고지서에 포함할 예약 상태들 (취소 제외 전부)
            List<Status> billableStatuses = List.of(Status.CONFIRMED, Status.INUSE, Status.COMPLETED);

            List<Reservation> reservations =
                    reservationRepository.findMonthlyManagementFeeReservations(
                            ho.getId(),
                            PaymentMethod.MANAGEMENT_FEE,
                            billableStatuses, // 상태 조건 확장
                            startOfMonth,
                            endOfMonth
                    );

            for (Reservation reservation : reservations) {
                // 시설 이름을 가져오려면 Space 엔티티가 필요하므로 name을 동적으로 구성
                String spaceName = (reservation.getSpace() != null) ? reservation.getSpace().getName() : "커뮤니티 시설";
                String itemName = String.format("%s 이용료", spaceName);

                BillItem item = BillItem.builder()
                        .itemType(BillItemType.COMMUNITY)
                        .referenceId(reservation.getId())
                        .name(itemName)
                        .price(BigDecimal.valueOf(reservation.getTotalPrice()))
                        .build();

                bill.addItem(item);
                hasAnyItem = true;
            }

//            List<Reservation> reservations =
//                    reservationRepository.findByMember_Resident_HoIdAndPaymentMethodAndStatusAndStartTimeBetween(
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


            // 해당 세대(Ho)의 멤버들이 예약한 내역 중
            // 결제 수단이 '관리비'이고, 상태가 '취소'가 아닌 '시작 시간' 기준 해당 월 내역 조회
//            List<Status> targetStatuses = List.of(Status.CONFIRMED, Status.INUSE, Status.COMPLETED);
//
//            List<Reservation> reservations =
//                    reservationRepository.findMonthlyManagementFeeReservations(
//                            ho.getId(),
//                            PaymentMethod.MANAGEMENT_FEE,
//                            targetStatuses,
//                            startOfMonth,
//                            endOfMonth
//                    );
//
//            for (Reservation reservation : reservations) {
//                // 예약한 시설명(Space Name)을 포함하면 입주민이 더 알아보기 쉽습니다.
//                String itemName = String.format("커뮤니티(%s) 이용료", reservation.getSpace().getName());
//
//                BillItem item = BillItem.builder()
//                        .itemType(BillItemType.COMMUNITY)
//                        .referenceId(reservation.getId())
//                        .name(itemName)
//                        // int totalPrice를 BigDecimal로 변환
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

    /**
     * [단계 2] 고지서 확정 및 실제 발행 (READY)
     * 관리자가 실제 종이 고지서를 돌리거나 앱으로 알림을 보낼 때 호출 (발행일 유연성 확보)
     */
    // [READY 단계] 입주민 공개 및 마감일 확정
    public void publishBills(Long apartmentId, String month) {
        List<Bill> bills = billRepository.findByHo_Dong_Apartment_IdAndBillMonth(apartmentId, month);

        // 보통 발행일로부터 익월 10일까지를 납기일로 설정하는 사례가 많음
        LocalDate dueDate = LocalDate.now().plusMonths(1).withDayOfMonth(10);

        for (Bill bill : bills) {
            if (bill.getStatus() == BillStatus.OPEN) {
                bill.markAsReady(dueDate); // READY 상태로 변경 및 readyAt 기록
            }
        }
    }

    /**
     * 매일 00:01에 실행: 납부 마감일이 지난 고지서를 OVERDUE로 변경
     */
    @Scheduled(cron = "0 1 0 * * ?") // 00:01 실행
    public void checkOverdueBills() {
        LocalDate today = LocalDate.now();

        // 오늘보다 이전(Before)이 마감일인데 아직 READY인 것들
        List<Bill> overdueBills = billRepository.findByStatusAndDueDateBefore(BillStatus.READY, today);

        for (Bill bill : overdueBills) {
            bill.markAsOverdue();
        }

        // (선택 사항) 로그 출력
        if (!overdueBills.isEmpty()) {
            System.out.println(today + " 기준 " + overdueBills.size() + "건의 고지서가 연체 처리되었습니다.");
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

