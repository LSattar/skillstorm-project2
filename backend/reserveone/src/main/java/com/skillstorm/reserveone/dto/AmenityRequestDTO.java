package com.skillstorm.reserveone.dto;

import com.skillstorm.reserveone.models.Amenity.Category;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AmenityRequestDTO(
    @NotBlank
    @Size(max = 120)
    String name,

    @NotNull
    Category category,

    Boolean isActive
) {
}

