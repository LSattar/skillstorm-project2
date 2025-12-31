package com.skillstorm.reserveone.dto.roles;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateRoleRequest(
                @NotBlank @Size(max = 40) String name) {
}
