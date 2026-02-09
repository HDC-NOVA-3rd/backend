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
        boolean p = Boolean.TRUE.equals(power);

        if (!p) {
            this.power = false;
            this.updatedAt = LocalDateTime.now();
            return;
        }

        // LED만 밝기 기준으로 ON 여부 결정
        if (this.type == DeviceType.LED) {
            int b = (this.brightness == null) ? 0 : this.brightness;
            this.power = b > 0;
        } else {
            this.power = true;
        }

        this.updatedAt = LocalDateTime.now();
    }


    public void changeBrightness(Integer brightness) {
        if (brightness == null) {
            this.brightness = null; // LED 아닌 경우
            this.updatedAt = LocalDateTime.now();
            return;
        }

        int b = Math.max(0, Math.min(100, brightness));
        this.brightness = b;

        // 밝기 0이면 OFF, 1~100이면 ON
        this.power = b > 0;

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
