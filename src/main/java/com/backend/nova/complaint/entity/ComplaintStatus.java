package com.backend.nova.complaint.entity;

import java.util.Arrays;
import java.util.List;

public enum ComplaintStatus {
    RECEIVED("접수"),
    ASSIGNED("담당자 배정"),
    IN_PROGRESS("처리 중"),
    COMPLETED("완료"),
    CANCELLED("취소");

    private final String description;

    ComplaintStatus(String description) {
        this.description = description;
    }

    public String getDescription() { return description; }

    // 단순히 "이 상태에서 저 상태로 갈 수 있는 로직인가?"라는 상태 규칙만 보유
    public boolean canChangeTo(ComplaintStatus next) {
        if (next == null || next == this) return false;
        if (next == CANCELLED) return this != COMPLETED;

        return switch (this) {
            case RECEIVED -> next == ASSIGNED;
            case ASSIGNED -> next == IN_PROGRESS;
            case IN_PROGRESS -> next == COMPLETED;
            default -> false;
        };
    }

    public List<ComplaintStatus> getNextAvailableStatuses() {
        return Arrays.stream(ComplaintStatus.values())
                .filter(this::canChangeTo)
                .toList();
    }
}