package com.skillstorm.reserveone.controllers;

import java.net.URI;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.skillstorm.reserveone.dto.roles.CreateRoleRequest;
import com.skillstorm.reserveone.dto.roles.RoleResponse;
import com.skillstorm.reserveone.services.RoleService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/roles")
@Validated
public class RoleController {

    private final RoleService service;

    public RoleController(@NonNull RoleService service) {
        this.service = Objects.requireNonNull(service, "service must not be null");
    }

    // CREATE
    @PostMapping
    public ResponseEntity<RoleResponse> create(
            @Valid @NonNull @RequestBody CreateRoleRequest req) {
        RoleResponse created = service.create(req);

        UUID id = Objects.requireNonNull(
                created.roleId(),
                "created.roleId must not be null");

        URI location = Objects.requireNonNull(
                ServletUriComponentsBuilder.fromCurrentRequest()
                        .path("/{id}")
                        .buildAndExpand(id)
                        .toUri(),
                "location URI must not be null");

        return ResponseEntity.created(location).body(created);
    }

    // READ (by ID)
    @GetMapping("/{roleId}")
    public ResponseEntity<RoleResponse> getById(@NonNull @PathVariable UUID roleId) {
        return ResponseEntity.ok(service.getById(roleId));
    }

    // READ (all roles)
    @GetMapping
    public ResponseEntity<List<RoleResponse>> getAll() {
        return ResponseEntity.ok(service.getAll());
    }

    // DELETE
    @DeleteMapping("/{roleId}")
    public ResponseEntity<Void> delete(@NonNull @PathVariable UUID roleId) {
        service.delete(roleId);
        return ResponseEntity.noContent().build();
    }
}