package com.skillstorm.fincen_project2_backend.dtos;

import java.util.UUID;

public record RoomTypeAmenityResponseDTO(
    UUID roomTypeId,
    UUID amenityId
) {
}
