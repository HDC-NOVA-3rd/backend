package com.backend.nova.bill.entity;

import com.backend.nova.apartment.entity.Ho;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "bill",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"ho_id", "month"})
        }
)

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class Bill {
    //확정된 월별 청구서

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 세대 식별자 (ho_id)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ho_id", nullable = false)
    private Ho ho;

    // 고지서 UUID
    @Column(name = "bill_uid", nullable = false, unique = true)
    private String billUid;

    // 청구월 (YYYY-MM)
    @Column(name = "bill_month", nullable = false, length = 7)
    private String billMonth; // 부과 대상월 (예: 2026-01)

    // 총 금액
    @Column(nullable = false)
    @Builder.Default
    private BigDecimal totalPrice = BigDecimal.ZERO;

    // 납부 상태
    @Enumerated(EnumType.STRING)
    private BillStatus status;

    //시스템이 계산을 시작한 날짜 (투명성)
    private LocalDateTime openAt;

    //관리자가(혹은 시스템 설정이) 고지서를 공표한 날짜 (발행일)
    private LocalDateTime readyAt;

    //발행일로부터 정확히 N일 뒤 (납기일 준수)
    @Column(nullable = false)
    private LocalDate dueDate;   // 납부 마감일 (발행일에 따라 유동적 설정)

    //고지서 “결제” 시점
    private LocalDateTime paidAt;

    //DB row 생성 시점
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    //DB row 생성 시점
    @Column(nullable = false, updatable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "bill", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<BillItem> items = new ArrayList<>();

    public void addItem(BillItem item) {
        item.assignBill(this);
        items.add(item);
        this.totalPrice = this.totalPrice.add(item.getPrice());
    }


    public void updateTotalPrice(BigDecimal totalPrice) {
        this.totalPrice = totalPrice;
    }

    public void markAsReady(LocalDate customDueDate) {
        this.status = BillStatus.READY;
        this.readyAt = LocalDateTime.now(); // 실제 확정/발행 버튼을 누른 시점
        this.dueDate = customDueDate;      // 관리자가 지정한 납부 마감일
    }

    //매일 자정 dueDate가 지난 READY 건들을 OVERDUE로 강제 전환.
    public void markAsOverdue() {
        if (this.status == BillStatus.READY) {
            this.status = BillStatus.OVERDUE;
            // 필요하다면 연체 기록 시점 필드를 추가해 기록할 수 있습니다.
            // this.updatedAt = LocalDateTime.now(); // @PreUpdate가 있다면 자동 처리됨
        }
    }

    public void markAsPaid() {
        this.status = BillStatus.PAID;
        this.paidAt = LocalDateTime.now();
    }

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }


}