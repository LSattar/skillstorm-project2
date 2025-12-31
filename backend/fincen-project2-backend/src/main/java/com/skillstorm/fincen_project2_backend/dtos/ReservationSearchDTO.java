package com.skillstorm.fincen_project2_backend.dtos;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import com.skillstorm.fincen_project2_backend.models.Reservation.Status;

public record ReservationSearchDTO(
    // User/Admin filters
    UUID userId,
    UUID hotelId,
    UUID roomId,
    UUID roomTypeId,

    // Status filtering
    List<Status> statuses,

    // Date range filters (reservation dates)
    LocalDate startDateFrom,
    LocalDate startDateTo,
    LocalDate endDateFrom,
    LocalDate endDateTo,

    // Creation date filters (when reservation was made)
    OffsetDateTime createdFrom,
    OffsetDateTime createdTo,

    // Guest count
    Integer minGuestCount,
    Integer maxGuestCount,

    // Price range
    BigDecimal minTotalAmount,
    BigDecimal maxTotalAmount,

    // Cancellation filters
    Boolean cancelledOnly,
    Boolean notCancelled,

    // Pagination (optional, for large result sets)
    Integer page,
    Integer size
) {
}

