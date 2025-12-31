package com.skillstorm.fincen_project2_backend.dtos;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import com.skillstorm.fincen_project2_backend.models.ReservationHold.Status;

public record ReservationHoldSearchDTO(
    // User/Admin filters
    UUID userId,
    UUID hotelId,
    UUID roomId,

    // Status filtering
    List<Status> statuses,

    // Date range filters (hold dates)
    LocalDate startDateFrom,
    LocalDate startDateTo,
    LocalDate endDateFrom,
    LocalDate endDateTo,

    // Expiration filters
    OffsetDateTime expiresBefore,
    OffsetDateTime expiresAfter,

    // Creation date filters
    OffsetDateTime createdFrom,
    OffsetDateTime createdTo,

    // Special filters
    Boolean expiredOnly,
    Boolean activeOnly,
    Boolean expiringSoon // holds expiring within next 24 hours
) {
}

