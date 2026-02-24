package com.backend.nova.complaint.entity;

import com.backend.nova.member.entity.Member;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Table(name = "complaint_review")
public class ComplaintReview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 민원
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "complaint_id", nullable = false)
    private Complaint complaint;

    // 작성자
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    // 피드백 내용
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    // 별점 (0~5)
    @DecimalMin(value = "0.0")
    @DecimalMax(value = "5.0")
    @Digits(integer = 1, fraction = 1) // 정수 1자리, 소수 1자리 (0.0~5.0)
    @Column(nullable = false, precision = 2, scale = 1)
    private BigDecimal rating;

    //소프트 딜리트용
    @Builder.Default
    @Column(nullable = false)
    private boolean deleted = false;

    // 등록일
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
