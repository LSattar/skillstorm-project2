package com.skillstorm.reserveone.dto.users;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateUserRequest(
        @NotBlank @Size(max = 120) String firstName,
        @NotBlank @Size(max = 120) String lastName,
        @NotBlank @Email @Size(max = 255) String email,
        @Size(max = 30) String phone,
        @Size(max = 200) String address1,
        @Size(max = 200) String address2,
        @Size(max = 60) String city,
        @Size(max = 2) String state,
        @Size(max = 10) String zip) {
}
