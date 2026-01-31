package com.backend.nova.apartmentcurrentbill.entity;

import com.backend.nova.apartment.entity.Ho;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "current_bill")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class CurrentBill {

    //이번 달 실시간 청구서

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 세대 식별자
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ho_id", nullable = false)
    private Ho ho;

    @Column(nullable = false, unique = true)
    private UUID billUuid;

    @Column(nullable = false, length = 7)
    private String billingMonth;

    @Column(nullable = false)
    private Integer totalAmount;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "currentBill", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<CurrentBillItem> items = new ArrayList<>();

    public void addItem(CurrentBillItem item) {
        items.add(item);
        item.setCurrentBill(this); // 양방향 관계 동기화
    }


}