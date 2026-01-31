package com.backend.nova.complaint.entity;

import com.backend.nova.member.entity.Member;
import com.backend.nova.admin.entity.Admin;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Table(name = "complaint")
public class Complaint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 작성자
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    // 담당 관리자 (nullable)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admin_id")
    private Admin admin;

    // 민원 유형
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ComplaintType type;

    // 민원 상태
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ComplaintStatus status;

    // 제목
    @Column(nullable = false, length = 255)
    private String title;

    // 내용
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    // 등록일
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // 수정일
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    /* ================== 생성/수정 로직 ================== */

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
        this.status = ComplaintStatus.RECEIVED; // 기본값: 민원 접수
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public void update(String title, String content, ComplaintType type) {
        this.title = title;
        this.content = content;
        this.type = type;
    }

    public void assignAdmin(Admin admin) {
        if (this.status != ComplaintStatus.RECEIVED) {
            throw new IllegalStateException("접수 상태에서만 관리자 배정 가능");
        }
        this.admin = admin;
        this.status = ComplaintStatus.ASSIGNED;
    }


    public void changeStatus(ComplaintStatus nextStatus) {
        if (!this.status.canChangeTo(nextStatus)) {
            throw new IllegalStateException(
                    "상태 변경 불가: " + this.status + " -> " + nextStatus
            );
        }
        this.status = nextStatus;
    }

}
