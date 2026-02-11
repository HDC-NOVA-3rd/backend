package com.backend.nova.reservation.dto;

import com.backend.nova.reservation.entity.PaymentMethod;
import com.backend.nova.reservation.entity.Reservation;
import com.backend.nova.reservation.entity.Status;

import java.time.LocalDateTime;

public record ReservationRequest(
        Long spaceId,
        LocalDateTime startTime,
        LocalDateTime endTime,
        int capacity,
        String ownerName,
        String ownerPhone,
        PaymentMethod paymentMethod
) {
}
