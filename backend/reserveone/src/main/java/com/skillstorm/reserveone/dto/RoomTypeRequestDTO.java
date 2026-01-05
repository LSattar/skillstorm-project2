package com.skillstorm.reserveone.dto;

import java.math.BigDecimal;
import java.util.UUID;

import com.skillstorm.reserveone.models.RoomType.BedType;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RoomTypeRequestDTO(
    @NotNull
    UUID hotelId,

    @NotBlank
    @Size(max = 120)
    String name,

    @Size(max = 2000)
    String description,

    @NotNull
    @DecimalMin(value = "0.0", inclusive = true)
    BigDecimal basePrice,

    @NotNull
    @Min(1)
    Integer maxGuests,

    @Min(1)
    Integer bedCount,

    BedType bedType,

    Boolean isActive
) {
}

