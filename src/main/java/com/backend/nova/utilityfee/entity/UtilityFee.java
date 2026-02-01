package com.backend.nova.utilityfee.entity;

import com.backend.nova.apartment.entity.Ho;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.time.YearMonth;

@Entity
@Table(
        name = "utility_fee",
        uniqueConstraints = {
                @UniqueConstraint(
                        columnNames = {"unitId", "meterType", "month"}
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class UtilityFee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 세대 식별자
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ho_id", nullable = false)
    private Ho ho;

    @Enumerated(EnumType.STRING)
    private MeterType meterType;

    private Long usage; // 이번 달 사용량

    private Integer calculatedFee;

    private YearMonth month;

    private String calculationBasis; // 단가/누진 버전

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}