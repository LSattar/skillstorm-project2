package com.skillstorm.reserveone.dto;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;

public record ReservationHoldRequestDTO(
    @NotNull
    UUID hotelId,

    @NotNull
    UUID roomId,

    @NotNull
    UUID userId,

    @NotNull
    LocalDate startDate,

    @NotNull
    LocalDate endDate,

    @NotNull
    OffsetDateTime expiresAt
) {
}

