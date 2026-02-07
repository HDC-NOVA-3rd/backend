package com.backend.nova.homeEnvironment.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor
@Table(
        name = "device",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"device_code"}) // 디바이스 고정 식별자 유니크
        }
)
public class Device {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 설치된 방
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    // 예: "light-1", "aircon-1", 기기 고유 코드
    @Column(name = "device_code", nullable = false, length = 50)
    private String deviceCode;

    // 예: "거실 전등", "거실 에어컨", 화면에 보여줄 이름
    @Column(nullable = false, length = 50)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DeviceType type;

    // 전원 상태 (센서는 null 가능이라고 했지만, MVP에선 디바이스만 넣는 걸 추천)
    @Column
    private Boolean power;

    // LED 전용(0~100) - LED 아니면 null
    @Column
    private Integer brightness;

    // AIRCON 전용 - 아니면 null
    @Column(name = "target_temp")
    private Integer targetTemp;

    // 상태가 마지막으로 갱신된 시각
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // ====== 생성/수정 편의 메서드 ======

    public static Device create(Room room, String deviceCode, String name, DeviceType type) {
        Device d = new Device();
        d.room = room;
        d.deviceCode = deviceCode;
        d.name = name;
        d.type = type;
        d.power = false;           //    기본 OFF
        d.updatedAt = LocalDateTime.now();
        return d;
    }

    public void setPower(Boolean power) {
        this.power = power;
        touch();
    }

    public void setBrightness(Integer brightness) {
        if (brightness == null) {
            this.brightness = null;
        } else {
            int v = Math.max(0, Math.min(100, brightness));
            this.brightness = v;
        }
        touch();
    }


    public void setTargetTemp(Integer targetTemp) {
        this.targetTemp = targetTemp;
        touch();
    }

    private void touch() {
        this.updatedAt = LocalDateTime.now();
    }

    // 이 객체가 DB에 처음 저장되기 직전에 자동으로 한 번 실행되는 메서드
    @PrePersist
    public void prePersist() {
        if (updatedAt == null) updatedAt = LocalDateTime.now();
        if (power == null) power = false;
    }
}
