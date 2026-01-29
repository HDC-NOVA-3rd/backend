package com.backend.nova.safety.entity;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

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

    @Builder
    public SafetySensorLog(SafetySensor safetySensor, Double value) {
        this.safetySensor = safetySensor;
        this.value = value;
    }

}
