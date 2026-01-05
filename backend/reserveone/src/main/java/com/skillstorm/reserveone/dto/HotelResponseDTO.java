package com.skillstorm.reserveone.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record HotelResponseDTO(
    UUID hotelId,
    String name,
    String phone,
    String address1,
    String address2,
    String city,
    String state,
    String zip,
    String timezone,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
) {
}

