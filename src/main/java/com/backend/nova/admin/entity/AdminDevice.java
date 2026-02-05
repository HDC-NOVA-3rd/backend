package com.backend.nova.admin.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
        name = "admin_device",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_admin_device",
                        columnNames = {"admin_id", "device_id"}
                )
        }
)
public class AdminDevice {

    @Id @GeneratedValue
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    private Admin admin;

    @Column(name = "device_id", nullable = false, length = 100)
    private String deviceId;

    private String lastIp;

    @Column(name = "trusted", nullable = false)
    private boolean trusted;

    @Column(name = "last_verified_at")
    private LocalDateTime lastVerifiedAt;

    @Column(name = "last_used_at")
    private LocalDateTime lastUsedAt;

    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
