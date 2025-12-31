package com.skillstorm.fincen_project2_backend.dtos;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

import com.skillstorm.fincen_project2_backend.models.ReservationHold.Status;

public record ReservationHoldResponseDTO(
    UUID holdId,
    UUID hotelId,
    UUID roomId,
    UUID userId,
    LocalDate startDate,
    LocalDate endDate,
    Status status,
    OffsetDateTime expiresAt,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
) {
}

