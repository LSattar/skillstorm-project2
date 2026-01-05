package com.skillstorm.reserveone.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.skillstorm.reserveone.models.Room.Status;

public record RoomResponseDTO(
    UUID roomId,
    UUID hotelId,
    UUID roomTypeId,
    String roomNumber,
    String floor,
    Status status,
    String notes,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
) {
}

