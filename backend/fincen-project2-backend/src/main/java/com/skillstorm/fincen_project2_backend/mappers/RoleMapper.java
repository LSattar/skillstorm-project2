package com.skillstorm.fincen_project2_backend.mappers;

import org.springframework.stereotype.Component;

import com.skillstorm.fincen_project2_backend.dto.roles.CreateRoleRequest;
import com.skillstorm.fincen_project2_backend.dto.roles.RoleResponse;
import com.skillstorm.fincen_project2_backend.models.Role;

@Component
public class RoleMapper {
    // CREATE
    public Role toEntity(CreateRoleRequest req) {
        if (req == null) {
            return null;
        }

        return new Role(req.name());
    }

    // READ
    public RoleResponse toResponse(Role role) {
        if (role == null) {
            return null;
        }

        return new RoleResponse(role.getRoleId(), role.getName());
    }
}
