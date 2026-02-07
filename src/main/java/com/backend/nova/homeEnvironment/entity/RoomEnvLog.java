package com.backend.nova.homeEnvironment.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "room_env_log")
public class RoomEnvLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @Column(name = "sensor_type", nullable = false)
    private String sensorType;

    @Column(name = "sensor_value", nullable = false)
    private Integer sensorValue;

    @Column(nullable = false)
    private String unit;

    @Column(name = "recorded_at", nullable = false)
    private LocalDateTime recordedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    // ✅ 저장용 팩토리 메서드 추가
    public static RoomEnvLog create(Room room, String sensorType, Integer sensorValue, String unit, LocalDateTime recordedAt) {
        RoomEnvLog log = new RoomEnvLog();
        log.room = room;
        log.sensorType = sensorType;
        log.sensorValue = sensorValue;
        log.unit = unit;
        log.recordedAt = recordedAt;
        return log;
    }

    // ✅ createdAt/recordedAt 자동 세팅 (null 방지)
    @PrePersist
    public void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (recordedAt == null) recordedAt = LocalDateTime.now();
        if (unit == null) unit = "";
    }
}
