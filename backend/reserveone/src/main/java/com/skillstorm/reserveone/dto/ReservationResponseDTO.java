package com.skillstorm.reserveone.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

import com.skillstorm.reserveone.models.Reservation.Status;

public record ReservationResponseDTO(
    UUID reservationId,
    UUID hotelId,
    UUID userId,
    UUID roomId,
    UUID roomTypeId,
    LocalDate startDate,
    LocalDate endDate,
    Integer guestCount,
    Status status,
    BigDecimal totalAmount,
    String currency,
    String specialRequests,
    String cancellationReason,
    OffsetDateTime cancelledAt,
    UUID cancelledByUserId,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
) {
}

