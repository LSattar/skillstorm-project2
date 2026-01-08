package com.skillstorm.reserveone.controllers;

import java.net.URI;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.skillstorm.reserveone.dto.users.CreateUserRequest;
import com.skillstorm.reserveone.dto.users.UpdateUserRequest;
import com.skillstorm.reserveone.dto.users.UpdateUserRolesRequest;
import com.skillstorm.reserveone.dto.users.UpdateUserStatusRequest;
import com.skillstorm.reserveone.dto.users.UserResponse;
import com.skillstorm.reserveone.services.UserService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/users")
@Validated
public class UserController {

    private final UserService service;

    public UserController(@NonNull UserService service) {
        this.service = Objects.requireNonNull(service, "service must not be null");
    }

    /**
     * Extracts localUserId from the authenticated principal. Supports
     * Jwt/OidcUser/OAuth2User.
     * Returns a UUID or throws a ResponseStatusException with an appropriate HTTP
     * status.
     */
    private static @NonNull UUID requireLocalUserId(@NonNull Object principal) {

        Object raw = null;

        // Resource Server / JWT principal
        if (principal instanceof Jwt jwt) {
            raw = jwt.getClaim("localUserId");
            if (raw == null)
                raw = jwt.getClaim("userId");
            if (raw == null)
                raw = jwt.getClaim("user_id");
        }
        // OIDC login principal
        else if (principal instanceof OidcUser oidc) {
            raw = oidc.getAttribute("localUserId");
            if (raw == null)
                raw = oidc.getAttribute("userId");
            if (raw == null)
                raw = oidc.getAttribute("user_id");
        }
        // OAuth2 login principal
        else if (principal instanceof OAuth2User oauth) {
            raw = oauth.getAttribute("localUserId");
            if (raw == null)
                raw = oauth.getAttribute("userId");
            if (raw == null)
                raw = oauth.getAttribute("user_id");
        }

        if (raw == null) {
            // Authenticated session exists, but missing the local user mapping
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Authenticated principal missing localUserId attribute.");
        }

        try {
            UUID id;
            if (raw instanceof UUID) {
                id = (UUID) raw;
            } else {
                id = UUID.fromString(String.valueOf(raw));
            }
            return Objects.requireNonNull(id, "localUserId must not be null");
        } catch (Exception e) {
            // Claim/attribute exists but is malformed
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "localUserId present but not a valid UUID: " + raw,
                    e);
        }
    }

    // GET /users/me
    @GetMapping("/me")
    public ResponseEntity<UserResponse> me(@AuthenticationPrincipal @NonNull Object principal) {
        UUID userId = requireLocalUserId(principal);
        return ResponseEntity.ok(service.getById(userId));
    }

    // PATCH /users/me (profile fields only)
    @PatchMapping("/me")
    public ResponseEntity<UserResponse> updateMyProfile(
            @AuthenticationPrincipal @NonNull Object principal,
            @RequestBody(required = false) @Valid UpdateUserRequest req) {
        if (req == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Request body is required.");
        }
        UUID userId = requireLocalUserId(principal);
        return ResponseEntity.ok(service.updateProfile(userId, req));
    }

    @GetMapping("/search")
    public ResponseEntity<List<UserResponse>> search(
            @RequestParam(name = "q", defaultValue = "") String q,
            @RequestParam(name = "limit", defaultValue = "20") int limit) {
        return ResponseEntity.ok(service.search(q, limit));
    }

    @PatchMapping("/{userId}/roles")
    public ResponseEntity<UserResponse> updateRoles(
            @NonNull @PathVariable UUID userId,
            @NonNull @Valid @RequestBody UpdateUserRolesRequest req) {
        return ResponseEntity.ok(service.updateRoles(userId, req));
    }

    // CREATE
    @PostMapping
    public ResponseEntity<UserResponse> create(@Valid @NonNull @RequestBody CreateUserRequest req) {
        UserResponse created = service.create(req);

        UUID id = Objects.requireNonNull(created.userId(), "created.userId must not be null");

        URI location = Objects.requireNonNull(
                ServletUriComponentsBuilder.fromCurrentRequest()
                        .path("/{id}")
                        .buildAndExpand(id)
                        .toUri(),
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
    public ResponseEntity<UserResponse> updateProfile(
            @NonNull @PathVariable UUID userId,
            @RequestBody(required = false) @Valid UpdateUserRequest req) {
        if (req == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Request body is required.");
        }
        return ResponseEntity.ok(service.updateProfile(userId, req));
    }

    // PATCH /users/{userId}/status
    @PatchMapping("/{userId}/status")
    public ResponseEntity<UserResponse> updateStatus(
            @NonNull @PathVariable UUID userId,
            @NonNull @Valid @RequestBody UpdateUserStatusRequest req) {
        return ResponseEntity.ok(service.updateStatus(userId, req));
    }

    // DELETE /users/{userId}
    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> delete(@NonNull @PathVariable UUID userId) {
        service.delete(userId);
        return ResponseEntity.noContent().build();
    }
}
