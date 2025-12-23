package com.skillstorm.fincen_project2_backend.dtos;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.skillstorm.fincen_project2_backend.models.Room.Status;

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
