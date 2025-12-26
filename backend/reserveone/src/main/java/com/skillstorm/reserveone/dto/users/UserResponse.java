package com.skillstorm.reserveone.dto.users;

import java.time.OffsetDateTime;
import java.util.Set;
import java.util.UUID;

import com.skillstorm.reserveone.models.User.Status;

public record UserResponse(
        UUID userId,
        String firstName,
        String lastName,
        String email,
        String phone,
        String address1,
        String address2,
        String city,
        String state,
        String zip,
        Status status,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        Set<String> roles) {
}
