package com.backend.nova.homeEnvironment.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Table(
        name = "device",
        uniqueConstraints = @UniqueConstraint(columnNames = {"device_code"})
)
public class Device {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @Column(name = "device_code", nullable = false, length = 50)
    private String deviceCode;

    @Column(nullable = false, length = 50)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DeviceType type;

    @Column(nullable = false)
    private Boolean power;

    private Integer brightness;

    @Column(name = "target_temp")
    private Integer targetTemp;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public void changePower(Boolean power) {
        this.power = Boolean.TRUE.equals(power);
        this.updatedAt = LocalDateTime.now();
    }

    public void changeBrightness(Integer brightness) {
        // null 허용(LED 아닌 경우)
        if (brightness == null) {
            this.brightness = null;
            this.updatedAt = LocalDateTime.now();
            return;
        }

        int v = Math.max(0, Math.min(100, brightness));
        this.brightness = v;

        // 밝기 1 이상이면 자동 ON, 0이면 OFF
        this.power = (v > 0);

        this.updatedAt = LocalDateTime.now();
    }

    public void changeTargetTemp(Integer targetTemp) {
        this.targetTemp = targetTemp;
        this.updatedAt = LocalDateTime.now();
    }

    @PrePersist // JPA가 INSERT 하기 직전 딱 한 번 실행
    void prePersist() { 
        if (updatedAt == null) updatedAt = LocalDateTime.now();
        if (power == null) power = false;
    }
}
