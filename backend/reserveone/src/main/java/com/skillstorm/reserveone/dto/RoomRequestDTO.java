package com.skillstorm.reserveone.dto;

import java.util.UUID;

import com.skillstorm.reserveone.models.Room.Status;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RoomRequestDTO(
    @NotNull
    UUID hotelId,

    @NotNull
    UUID roomTypeId,

    @NotBlank
    @Size(max = 20)
    String roomNumber,

    @Size(max = 20)
    String floor,

    Status status,

    @Size(max = 2000)
    String notes
) {
}

