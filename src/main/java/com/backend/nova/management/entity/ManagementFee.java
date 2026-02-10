package com.backend.nova.management.entity;

import com.backend.nova.apartment.entity.Apartment;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "management_fee",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_management_fee",
                        columnNames = {"apartment_id", "name", "active"}
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class ManagementFee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 단지
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "apartment_id", nullable = false)
    private Apartment apartment;

    // 항목명
    @Column(nullable = false, length = 50)
    private String name;

    // 가격
    @Column(nullable = false)
    BigDecimal price;

    // 설명
    @Column(length = 255)
    private String description;

    // 사용 여부
    @Column(nullable = false)
    private boolean active;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Column(nullable = true)
    private LocalDateTime deactivatedAt;


    /* ===== 생성 ===== */
    public static ManagementFee create(
            Apartment apartment,
            String name,
            BigDecimal price,
            String description
    ) {
        return ManagementFee.builder()
                .apartment(apartment)
                .name(name)
                .price(price)
                .description(description)
                .active(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }


    /* ===== 수정 ===== */
    public void update(
            String name,
            BigDecimal price,
            String description
    ) {
        if (name != null) {
            this.name = name;
        }
        if (price != null) {
            this.price = price;
        }
        if (description != null) {
            this.description = description;
        }
        this.updatedAt = LocalDateTime.now();
    }


    /* ===== 삭제 ===== */
    public void deactivate() {
        if (!this.active) return;
        this.active = false;
        this.deactivatedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    /* ===== 복구 ===== */
    public void restore() {
        if (this.active) return;
        this.active = true;
        this.deactivatedAt = null;
        this.updatedAt = LocalDateTime.now();
    }


}
