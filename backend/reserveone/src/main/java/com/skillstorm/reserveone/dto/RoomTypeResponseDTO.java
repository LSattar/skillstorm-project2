package com.skillstorm.reserveone.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import com.skillstorm.reserveone.models.RoomType.BedType;

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

