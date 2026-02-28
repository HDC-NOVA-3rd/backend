package com.backend.nova.homeEnvironment.mode.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalTime;

@Entity
@Table(name = "mode_schedule")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class ModeSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 대상 모드
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mode_id", nullable = false)
    private Mode mode;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time")
    private LocalTime endTime;

    // 종료 시 실행할 모드(옵션)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "end_mode_id")
    private Mode endMode;

    // 예: "MON,WED,FRI" / "DAILY" / "WEEKDAY"
    @Column(name = "repeat_days", length = 50)
    private String repeatDays;

    @Column(name = "is_enabled", nullable = false)
    private boolean isEnabled;

    public void enable() { this.isEnabled = true; }
    public void disable() { this.isEnabled = false; }
}
