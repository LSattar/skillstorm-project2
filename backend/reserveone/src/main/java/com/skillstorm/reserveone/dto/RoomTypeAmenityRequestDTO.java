package com.skillstorm.reserveone.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

public record RoomTypeAmenityRequestDTO(
    @NotNull
    UUID roomTypeId,

    @NotNull
    UUID amenityId
) {
}

