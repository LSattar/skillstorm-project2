package com.skillstorm.fincen_project2_backend.mappers;

import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import com.skillstorm.fincen_project2_backend.dto.roles.CreateRoleRequest;
import com.skillstorm.fincen_project2_backend.dto.roles.RoleResponse;
import com.skillstorm.fincen_project2_backend.models.Role;

@Component
public class RoleMapper {

    // CREATE (DTO -> Entity)
    @Nullable
    public Role toEntity(@Nullable CreateRoleRequest req) {
        if (req == null) {
            return null;
        }
        return new Role(req.name());
    }

    // CREATE (normalized String -> Entity)
    @NonNull
    public Role toEntity(@NonNull String name) {
        return new Role(name);
    }

    // READ (Entity -> DTO)
    @Nullable
    public RoleResponse toResponse(@Nullable Role role) {
        if (role == null) {
            return null;
        }
        return new RoleResponse(role.getRoleId(), role.getName());
    }
}
