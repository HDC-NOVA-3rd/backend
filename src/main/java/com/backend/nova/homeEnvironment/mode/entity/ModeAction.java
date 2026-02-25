package com.backend.nova.homeEnvironment.mode.entity;

import com.backend.nova.homeEnvironment.entity.Device;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "mode_action",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"mode_id", "sort_order"})
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class ModeAction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 어떤 모드에 속하는지
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mode_id", nullable = false)
    private Mode mode;

    // 실행 순서
    // 유저가 만든 액션 순서를 항상 똑같이 저장/조회/표시
    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;

    // 어떤 디바이스를 제어할지
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_id", nullable = false)
    private Device device;

    @Enumerated(EnumType.STRING)
    @Column(name = "command", nullable = false, length = 30)
    private ModeActionCommand command;

    @Column(name = "value", length = 100)
    private String value;
}
