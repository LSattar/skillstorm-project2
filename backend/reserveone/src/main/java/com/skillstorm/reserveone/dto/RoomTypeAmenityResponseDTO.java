package com.skillstorm.reserveone.dto;

import java.util.UUID;

public record RoomTypeAmenityResponseDTO(
    UUID roomTypeId,
    UUID amenityId
) {
}

