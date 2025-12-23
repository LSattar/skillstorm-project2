package com.skillstorm.fincen_project2_backend.dtos;

import com.skillstorm.fincen_project2_backend.models.Amenity.Category;

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
