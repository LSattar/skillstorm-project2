package com.skillstorm.fincen_project2_backend.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record HotelRequestDTO(
    @NotBlank
    @Size(max = 200)
    String name,

    @NotBlank
    @Size(max = 30)
    String phone,

    @NotBlank
    @Size(max = 200)
    String address1,

    @Size(max = 200)
    String address2,

    @NotBlank
    @Size(max = 60)
    String city,

    @NotBlank
    @Size(min = 2, max = 2)
    String state,

    @NotBlank
    @Size(min = 5, max = 10)
    String zip,

    @Size(max = 80)
    String timezone
) {
}
