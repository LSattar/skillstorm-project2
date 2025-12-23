package com.skillstorm.fincen_project2_backend.dtos;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import com.skillstorm.fincen_project2_backend.models.RoomType.BedType;

public record RoomTypeResponseDTO(
    UUID roomTypeId,
    UUID hotelId,
    String name,
    String description,
    BigDecimal basePrice,
    Integer maxGuests,
    Integer bedCount,
    BedType bedType,
    Boolean isActive,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
) {
}
