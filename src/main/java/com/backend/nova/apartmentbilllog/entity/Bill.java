package com.backend.nova.apartmentbilllog.entity;

import com.backend.nova.apartment.entity.Ho;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "bill")
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
    @Column(nullable = false, unique = true)
    private UUID billUuid;

    // 청구월 (YYYY-MM)
    @Column(nullable = false, length = 7)
    private String billingMonth;

    // 총 금액
    @Column(nullable = false)
    private Integer totalAmount;

    // 납부 상태 (true = 납부 완료, false = 미납)
    @Column(nullable = false)
    private boolean status;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "bill", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<BillItem> items = new ArrayList<>();

    public void addItem(BillItem item) {
        items.add(item);
        item.setBill(this); // 양방향 관계 동기화
    }


}