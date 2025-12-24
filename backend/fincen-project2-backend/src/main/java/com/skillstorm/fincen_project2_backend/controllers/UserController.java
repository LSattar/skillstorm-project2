package com.skillstorm.fincen_project2_backend.controllers;

import java.net.URI;
import java.util.Objects;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.skillstorm.fincen_project2_backend.dto.users.CreateUserRequest;
import com.skillstorm.fincen_project2_backend.dto.users.UpdateUserRequest;
import com.skillstorm.fincen_project2_backend.dto.users.UpdateUserStatusRequest;
import com.skillstorm.fincen_project2_backend.dto.users.UserResponse;
import com.skillstorm.fincen_project2_backend.services.UserService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/users")
@Validated
public class UserController {

    private final UserService service;

    public UserController(@NonNull UserService service) {
        this.service = Objects.requireNonNull(service, "service must not be null");
    }

    // CREATE
    @PostMapping
    public ResponseEntity<UserResponse> create(
            @Valid @NonNull @RequestBody CreateUserRequest req) {
        UserResponse created = service.create(req);

        UUID id = java.util.Objects.requireNonNull(
                created.userId(),
                "created.userId must not be null");

        URI location = Objects.requireNonNull(
                ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(id).toUri(),
                "location URI must not be null");

        return ResponseEntity.created(location).body(created);
    }

    // READ
    @GetMapping("/{userId}")
    public ResponseEntity<UserResponse> getById(@NonNull @PathVariable UUID userId) {
        return ResponseEntity.ok(service.getById(userId));
    }

    // PATCH /users/{userId} (profile fields only)
    @PatchMapping("/{userId}")
    public ResponseEntity<UserResponse> updateProfile(@NonNull @PathVariable UUID userId,
            @RequestBody(required = false) @Valid UpdateUserRequest req) {
        return ResponseEntity.ok(service.updateProfile(userId, req));
    }

    // PATCH /users/{userId}/status
    @PatchMapping("/{userId}/status")
    public ResponseEntity<UserResponse> updateStatus(@NonNull @PathVariable UUID userId,
            @Valid @RequestBody UpdateUserStatusRequest req) {
        return ResponseEntity.ok(service.updateStatus(userId, req));
    }

    // DELETE /users/{userId}
    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> delete(@NonNull @PathVariable UUID userId) {
        service.delete(userId);
        return ResponseEntity.noContent().build();
    }

}
