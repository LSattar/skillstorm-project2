package com.skillstorm.reserveone.dto.users;

import java.util.UUID;

import com.skillstorm.reserveone.models.User.Status;

public record UserSearchResponse(
        UUID userId,
        String firstName,
        String lastName,
        String email,
        Status status) {
}
