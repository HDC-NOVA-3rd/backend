package com.backend.nova.reservation.dto;

import com.backend.nova.reservation.entity.PaymentMethod;
import com.backend.nova.reservation.entity.Reservation;
import com.backend.nova.reservation.entity.Status;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record ReservationResponse(
        Long id,
        Long spaceId,
        String spaceName,
        LocalDateTime startTime,
        LocalDateTime endTime,
        int capacity,
        int totalPrice,
        String ownerName,
        String ownerPhone,
        PaymentMethod paymentMethod,
        String qrToken,
        Status status
) {
    public static ReservationResponse from(Reservation reservation) {
        return ReservationResponse.builder()
                .id(reservation.getId())
                .spaceId(reservation.getSpace().getId())
                .spaceName(reservation.getSpace().getName())
                .startTime(reservation.getStartTime())
                .endTime(reservation.getEndTime())
                .capacity(reservation.getCapacity())
                .totalPrice(reservation.getTotalPrice())
                .ownerName(reservation.getOwnerName())
                .ownerPhone(reservation.getOwnerPhone())
                .paymentMethod(reservation.getPaymentMethod())
                .qrToken(reservation.getQrToken())
                .status(reservation.getStatus())
                .build();
    }
}
