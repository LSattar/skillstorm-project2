package com.skillstorm.reserveone.dto.roles;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RoleResponse(
        @NotNull UUID roleId,
        @NotNull @Size(max = 40) String name) {
}
