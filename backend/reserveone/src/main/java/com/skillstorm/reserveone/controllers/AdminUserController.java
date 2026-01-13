package com.skillstorm.reserveone.controllers;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.skillstorm.reserveone.dto.users.UserSearchResponse;
import com.skillstorm.reserveone.services.UserService;

@RestController
@RequestMapping("/admin/users")
public class AdminUserController {

    private final UserService service;

    public AdminUserController(UserService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<Page<UserSearchResponse>> searchUsers(
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "ACTIVE") String status,
            Pageable pageable) {
        return ResponseEntity.ok(service.searchAdminUsers(q, status, pageable));
    }

    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<Void> deactivateUser(
            @PathVariable UUID id,
            Authentication auth) {
        UUID actingAdminId = UUID.fromString(auth.getName());
        service.deactivateUser(id, actingAdminId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/activate")
    public ResponseEntity<Void> activateUser(
            @PathVariable UUID id,
            Authentication auth) {
        UUID actingAdminId = UUID.fromString(auth.getName());
        service.activateUser(id, actingAdminId);
        return ResponseEntity.noContent().build();
    }
}
