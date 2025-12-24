package com.skillstorm.fincen_project2_backend.controllers;

import java.util.Objects;

import com.skillstorm.fincen_project2_backend.exceptions.ResourceConflictException;
import com.skillstorm.fincen_project2_backend.models.Role;
import com.skillstorm.fincen_project2_backend.models.User;
import com.skillstorm.fincen_project2_backend.repositories.UserRepository;
import com.skillstorm.fincen_project2_backend.services.RoleService;

import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@RestController
@RequestMapping("/auth")
@Validated
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RoleService roleService;

    public AuthController(
            @NonNull UserRepository userRepository,
            @NonNull PasswordEncoder passwordEncoder,
            @NonNull RoleService roleService) {
        this.userRepository = Objects.requireNonNull(userRepository, "userRepository must not be null");
        this.passwordEncoder = Objects.requireNonNull(passwordEncoder, "passwordEncoder must not be null");
        this.roleService = Objects.requireNonNull(roleService, "roleService must not be null");
    }

    @PostMapping("/register")
    @Transactional // CRITICAL: ensures roles are properly linked during user creation
    public ResponseEntity<Void> register(
            @Valid @NonNull @RequestBody(required = true) RegisterRequest request) {
        String email = normalizeEmail(request.email());

        if (userRepository.existsByEmail(email)) {
            throw new ResourceConflictException("Email is already registered.");
        }

        User user = new User(
                request.firstName().trim(),
                request.lastName().trim(),
                email);

        String hashedPassword = Objects.requireNonNull(
                passwordEncoder.encode(request.password()),
                "passwordEncoder.encode returned null");
        user.setPasswordHash(hashedPassword);

        Role guestRole = Objects.requireNonNull(
                roleService.getEntityByName("GUEST"),
                "ROLE_GUEST not found");
        user.addRole(guestRole);

        userRepository.save(user);

        return ResponseEntity.status(201).build();
    }

    private static String normalizeEmail(String email) {
        if (email == null) {
            throw new IllegalArgumentException("Email is required.");
        }
        String trimmed = email.trim();
        if (trimmed.isBlank()) {
            throw new IllegalArgumentException("Email is required.");
        }
        // With CITEXT, lowercasing isn't required, but trimming is.
        return trimmed;
    }

    public record RegisterRequest(
            @NotBlank @Size(max = 120) String firstName,
            @NotBlank @Size(max = 120) String lastName,
            @NotBlank @Email @Size(max = 255) String email,
            @NotBlank @Size(min = 8, max = 72) String password) {
    }
}
