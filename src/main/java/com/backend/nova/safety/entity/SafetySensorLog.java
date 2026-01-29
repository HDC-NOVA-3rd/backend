package com.backend.nova.safety.entity;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "sensor_log")
@Getter
@NoArgsConstructor
public class SafetySensorLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sensor_id", nullable = false)
    private SafetySensor safetySensor;

    @Column(name = "value", nullable = false)
    private Double value;

    @Column(name = "unit", nullable = false)
    private String unit;

    @Column(name = "evented_at", nullable = false)
    private LocalDateTime eventedAt;

    @Builder
    public SafetySensorLog(SafetySensor safetySensor, Double value, String unit, LocalDateTime eventedAt) {
        this.safetySensor = safetySensor;
        this.value = value;
        this.unit = unit;
        this.eventedAt = eventedAt;
    }

}
