package com.skillstorm.fincen_project2_backend.dtos;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

public record RoomTypeAmenityRequestDTO(
    @NotNull
    UUID roomTypeId,

    @NotNull
    UUID amenityId
) {
}
