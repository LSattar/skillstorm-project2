package com.skillstorm.reserveone.dto;

import java.util.UUID;

import com.skillstorm.reserveone.models.Amenity.Category;

public record AmenityResponseDTO(
    UUID amenityId,
    String name,
    Category category,
    Boolean isActive
) {
}

