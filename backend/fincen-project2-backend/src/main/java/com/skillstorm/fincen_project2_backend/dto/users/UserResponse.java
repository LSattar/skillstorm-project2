package com.skillstorm.fincen_project2_backend.dto.users;

import java.time.OffsetDateTime;
import java.util.Set;
import java.util.UUID;

import com.skillstorm.fincen_project2_backend.models.User.Status;

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

        // Optional: safe role names (don’t expose Role entity)
        Set<String> roles
) {}
