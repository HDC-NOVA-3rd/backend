package com.backend.nova.reservation.entity;

import com.backend.nova.facility.entity.Space;
import com.backend.nova.member.entity.Member;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // JPA 필수
@AllArgsConstructor
@Builder

public class Reservation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "space_id", nullable = false)
    private Space space;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;

    @Column(name = "capacity", nullable = false)
    private int capacity;

    @Column(name = "total_price")
    private int totalPrice;

    @Column(name = "owner_name")
    private String ownerName;

    @Column(name = "owner_phone")
    private String ownerPhone;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false)
    private PaymentMethod paymentMethod;

    @Column(name = "qr_token", nullable = false)
    private String qrToken;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private Status status;

    public void cancel() {
        if (this.status == Status.CANCELLED) {
            throw new IllegalStateException("이미 취소된 예약입니다.");
        }
        if (this.status == Status.COMPLETED) {
            throw new IllegalStateException("이미 완료된 예약은 취소할 수 없습니다.");
        }
        this.status = Status.CANCELLED;
    }


}
