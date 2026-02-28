package com.backend.nova.complaint.entity;

import java.util.Set;

public enum ComplaintStatus {

    RECEIVED,     // 접수
    ASSIGNED,     // 담당자 배정
    IN_PROGRESS,  // 처리 중
    COMPLETED;    // 완료

    public boolean canChangeTo(ComplaintStatus next) {
        return switch (this) {
            case RECEIVED -> Set.of(ASSIGNED).contains(next);
            case ASSIGNED -> Set.of(IN_PROGRESS).contains(next);
            case IN_PROGRESS -> Set.of(COMPLETED).contains(next);
            case COMPLETED -> false;
        };
    }
}
