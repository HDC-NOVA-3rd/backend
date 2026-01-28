package com.backend.nova.apartment.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"dong_id", "ho_no"}))
public class Ho {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dong_id", nullable = false)
    private Dong dong;

    @Column(name = "ho_no", nullable = false, length = 50)
    private String hoNo;

    private Integer floor;
}