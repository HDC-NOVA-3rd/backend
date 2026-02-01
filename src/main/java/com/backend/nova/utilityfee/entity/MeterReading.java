package com.backend.nova.utilityfee.entity;

import com.backend.nova.apartment.entity.Ho;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "meter_reading")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class MeterReading {

    //IoT 원천 데이터

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 세대 식별자
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ho_id", nullable = false)
    private Ho ho;

    @Enumerated(EnumType.STRING)
    private MeterType meterType; // WATER, ELECTRIC, GAS

    private Long readingValue; // 누적값

    private LocalDateTime collectedAt;

    @Enumerated(EnumType.STRING)
    private MeterSource source; // IOT, MANUAL

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
}