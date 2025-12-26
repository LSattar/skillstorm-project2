package com.skillstorm.reserveone.mappers;

import java.util.Objects;
import java.util.UUID;

import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import com.skillstorm.reserveone.dto.roles.CreateRoleRequest;
import com.skillstorm.reserveone.dto.roles.RoleResponse;
import com.skillstorm.reserveone.models.Role;

@Component
public class RoleMapper {

    @NonNull
    public Role toEntity(@NonNull CreateRoleRequest req) {
        Objects.requireNonNull(req, "req must not be null");
        return new Role(req.name()); // entity normalizes/validates (or service can pre-normalize)
    }

    @NonNull
    public Role toEntity(@NonNull String name) {
        Objects.requireNonNull(name, "name must not be null");
        return new Role(name);
    }

    @NonNull
    public RoleResponse toResponse(@NonNull Role role) {
        Objects.requireNonNull(role, "role must not be null");

        UUID id = Objects.requireNonNull(role.getRoleId(), "roleId must not be null");
        String name = Objects.requireNonNull(role.getName(), "role name must not be null");

        return new RoleResponse(id, name);
    }
}
