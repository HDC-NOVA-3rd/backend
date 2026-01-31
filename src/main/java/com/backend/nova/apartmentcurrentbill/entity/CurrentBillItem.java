package com.backend.nova.apartmentcurrentbill.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "current_bill_item")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class CurrentBillItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 어느 고지서의 항목인지
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "current_bill_id", nullable = false)
    private CurrentBill currentBill;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(nullable = false)
    private Integer price;

    @Column(nullable = false, length = 30)
    private String category;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public void setCurrentBill(CurrentBill currentBill) {
        this.currentBill = currentBill;
    }
}