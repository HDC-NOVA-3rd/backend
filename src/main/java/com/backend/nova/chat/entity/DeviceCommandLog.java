package com.backend.nova.chat.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "device_command_log")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class DeviceCommandLog {

    @Id
    @Column(name = "trace_id", length = 64)
    private String traceId;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "ho_id", nullable = false)
    private Long hoId;

    @Column(name = "room_id", nullable = false)
    private Long roomId;

    @Column(name = "device_id", nullable = false)
    private Long deviceId;

    @Column(name = "command", nullable = false, length = 32)
    private String command; // POWER_ON / POWER_OFF / SET_TEMP 등 (일단 문자열)

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private CommandStatus status;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    //새로운 요청이 들어올때 사용 .. stauts = pending 으로 시작하도록 강제
    public static DeviceCommandLog pending(
            String traceId,
            Long memberId,
            Long hoId,
            Long roomId,
            Long deviceId,
            String command
    ) {
        return DeviceCommandLog.builder()
                .traceId(traceId)
                .memberId(memberId)
                .hoId(hoId)
                .roomId(roomId)
                .deviceId(deviceId)
                .command(command)
                .status(CommandStatus.PENDING)
                .build();
    }

    public void markSuccess() {
        this.status = CommandStatus.SUCCESS;
    }

    public void markFailed() {
        this.status = CommandStatus.FAILED;
    }
}
