package com.backend.nova.safety.entity;

import com.backend.nova.apartment.entity.Apartment;
import com.backend.nova.safety.enums.SafetyReason;
import com.backend.nova.safety.enums.SafetyStatus;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "safety_status")
@Getter
@NoArgsConstructor
public class SafetyStatusEntity {

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

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "reason", nullable = false, length = 30)
    private SafetyReason reason;

    @Enumerated(EnumType.STRING)
    @Column(name = "safety_status", nullable = false, length = 20)
    private SafetyStatus safetyStatus;

    @Builder
    public SafetyStatusEntity(
            Apartment apartment,
            Long dongId,
            Long facilityId,
            LocalDateTime updatedAt,
            SafetyReason reason,
            SafetyStatus safetyStatus
    ) {
        this.apartment = apartment;
        this.dongId = dongId;
        this.facilityId = facilityId;
        this.updatedAt = updatedAt;
        this.reason = reason;
        this.safetyStatus = safetyStatus;
    }

    public void update(LocalDateTime updatedAt, SafetyReason reason, SafetyStatus safetyStatus) {
        this.updatedAt = updatedAt;
        this.reason = reason;
        this.safetyStatus = safetyStatus;
    }

}
