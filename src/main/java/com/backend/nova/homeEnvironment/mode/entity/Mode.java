package com.backend.nova.homeEnvironment.mode.entity;

import com.backend.nova.apartment.entity.Ho;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "mode",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"ho_id", "mode_name"})
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class Mode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 세대(호)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ho_id", nullable = false)
    private Ho ho;

    @Column(name = "mode_name", nullable = false, length = 100)
    private String modeName;

    // 기본 제공 모드 여부
    @Column(name = "is_default", nullable = false)
    private boolean isDefault;

    // 목록 표시 여부(숨김 기능)
    @Column(name = "is_visible", nullable = false)
    private boolean isVisible;

    // 수정 가능 여부(기본=false, 커스텀=true)
    @Column(name = "is_editable", nullable = false)
    private boolean isEditable;

    // 모드에 속한 액션들
    @OneToMany(mappedBy = "mode", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    @Builder.Default
    private List<ModeAction> actions = new ArrayList<>();

    // 모드에 속한 스케줄들
    @OneToMany(mappedBy = "mode", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ModeSchedule> schedules = new ArrayList<>();

    // ===== 도메인 메서드 =====
    public void assertEditable() {
        if (!isEditable) {
            throw new IllegalStateException("기본 모드는 수정할 수 없습니다.");
        }
    }

    public void hide() {
        this.isVisible = false;
    }

    public void show() {
        this.isVisible = true;
    }
}

