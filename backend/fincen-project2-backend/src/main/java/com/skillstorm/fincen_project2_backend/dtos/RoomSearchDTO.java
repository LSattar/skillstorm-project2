package com.skillstorm.fincen_project2_backend.dtos;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.skillstorm.fincen_project2_backend.models.RoomType.BedType;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record RoomSearchDTO(
    @NotNull
    LocalDate startDate,

    @NotNull
    LocalDate endDate,

    UUID hotelId,

    UUID roomTypeId,

    // Guest capacity
    @Min(1)
    Integer minGuests,

    // Price filtering
    BigDecimal minPricePerNight,
    BigDecimal maxPricePerNight,
    BigDecimal maxTotalPrice,

    // Bed preferences
    BedType bedType,
    @Min(1)
    Integer minBedCount,

    // Amenities (list of amenity IDs that must be present)
    List<UUID> requiredAmenityIds,

    // Only show active room types
    Boolean activeOnly
) {
    public RoomSearchDTO {
        if (activeOnly == null) {
            activeOnly = true; // Default to only active room types
        }
    }
}
