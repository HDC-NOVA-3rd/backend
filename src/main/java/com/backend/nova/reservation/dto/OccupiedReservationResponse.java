package com.backend.nova.reservation.dto;

import com.backend.nova.reservation.entity.Reservation;
import java.time.LocalDateTime;

public record OccupiedReservationResponse(
        LocalDateTime startTime,
        LocalDateTime endTime
) {
    public static OccupiedReservationResponse from(Reservation reservation) {
        return new OccupiedReservationResponse(
                reservation.getStartTime(),
                reservation.getEndTime()
        );
    }
}
