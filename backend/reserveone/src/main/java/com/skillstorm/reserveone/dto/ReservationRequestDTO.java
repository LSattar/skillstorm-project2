package com.skillstorm.reserveone.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import com.skillstorm.reserveone.models.Reservation.Status;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ReservationRequestDTO(
    @NotNull
    UUID hotelId,

    @NotNull
    UUID userId,

    @NotNull
    UUID roomId,

    @NotNull
    UUID roomTypeId,

    @NotNull
    LocalDate startDate,

    @NotNull
    LocalDate endDate,

    @NotNull
    @Min(1)
    Integer guestCount,

    Status status,

    @DecimalMin(value = "0.0", inclusive = true)
    BigDecimal totalAmount,

    @Size(min = 3, max = 3)
    String currency,

    @Size(max = 2000)
    String specialRequests
) {
}

