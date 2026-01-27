package com.backend.nova.safety.entity;

import com.backend.nova.apartment.entity.Apartment;
import com.backend.nova.safety.enums.SafetyStatus;
import com.backend.nova.safety.enums.SensorType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "safety_event_log")
@Getter
@NoArgsConstructor
public class SafetyEventLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "apartment_id", nullable = false)
    private Apartment apartment;

    @Column(name = "area", nullable = false)
    private String area;

    @Column(name = "request_from", nullable = false)
    private String requestFrom;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sensor_id")
    private Sensor sensor;

    @Enumerated(EnumType.STRING)
    @Column(name = "sensor_type", nullable = false, length = 20)
    private SensorType sensorType;

    @Column(name = "value")
    private Double value;

    @Column(name = "unit")
    private String unit;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_to", nullable = false, length = 20)
    private SafetyStatus statusTo;

    @Column(name = "event_at", nullable = false)
    private LocalDateTime eventAt;
}
