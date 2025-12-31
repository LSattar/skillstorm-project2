package com.skillstorm.fincen_project2_backend.dtos;

import java.util.UUID;

import com.skillstorm.fincen_project2_backend.models.Amenity.Category;

public record AmenityResponseDTO(
    UUID amenityId,
    String name,
    Category category,
    Boolean isActive
) {
}
