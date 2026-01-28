package com.backend.nova.safety.entity;

import com.backend.nova.apartment.entity.Apartment;
import com.backend.nova.safety.enums.SafetyStatus;
import com.backend.nova.safety.enums.SensorType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Builder;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Objects;

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

    @Column(name = "dong_id")
    private Long dongId;

    @Column(name = "facility_id")
    private Long facilityId;

    @Column(name = "manual", nullable = false)
    private boolean manual;

    @Column(name = "request_from", nullable = false)
    private String requestFrom;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sensor_id")
    private Sensor sensor;

    @Enumerated(EnumType.STRING)
    @Column(name = "sensor_type", length = 20)
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

    @Builder
    public SafetyEventLog(
            Apartment apartment,
            Long dongId,
            Long facilityId,
            boolean manual,
            String requestFrom,
            Sensor sensor,
            SensorType sensorType,
            Double value,
            String unit,
            SafetyStatus statusTo,
            LocalDateTime eventAt
    ) {
        this.apartment = apartment;
        this.dongId = dongId;
        this.facilityId = facilityId;
        this.manual = manual;
        this.requestFrom = requestFrom;
        this.sensor = sensor;
        this.sensorType = sensorType;
        this.value = value;
        this.unit = unit;
        this.statusTo = statusTo;
        this.eventAt = eventAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SafetyEventLog other)) return false;
        return id != null && Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "SafetyEventLog{id=" + id + "}";
    }
}
