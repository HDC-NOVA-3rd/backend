package com.backend.nova.bill.entity;

import com.backend.nova.apartment.entity.Ho;
import jakarta.persistence.*;
import lombok.*;
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

    // 고지서 UID
    @Column(name = "bill_uuid", nullable = false, unique = true)
    private String billUid;


    // 청구월 (YYYY-MM)
    @Column(nullable = false, length = 7)
    private String month;

    // 총 금액
    @Column(nullable = false)
    @Builder.Default
    private Integer totalPrice = 0;

    // 납부 상태
    @Enumerated(EnumType.STRING)
    private BillStatus status;

    //DB row 생성 시점
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    //고지서 “확정/발행” 시점
    private LocalDateTime issuedAt;

    @OneToMany(mappedBy = "bill", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<BillItem> items = new ArrayList<>();

    public void addItem(BillItem item) {
        item.assignBill(this);
        items.add(item);
        this.totalPrice += item.getPrice();
    }

    public void updateTotalPrice(int totalPrice) {
        this.totalPrice = totalPrice;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }



}