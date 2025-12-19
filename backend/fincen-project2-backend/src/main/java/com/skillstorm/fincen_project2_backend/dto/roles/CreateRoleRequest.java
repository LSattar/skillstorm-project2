package com.skillstorm.fincen_project2_backend.dto.roles;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateRoleRequest(
        @NotBlank @Size(max = 40) String name) {
}
