package com.skillstorm.fincen_project2_backend.services;

import java.util.Objects;
import java.util.UUID;

import com.skillstorm.fincen_project2_backend.dto.users.CreateUserRequest;
import com.skillstorm.fincen_project2_backend.dto.users.UpdateUserRequest;
import com.skillstorm.fincen_project2_backend.dto.users.UpdateUserStatusRequest;
import com.skillstorm.fincen_project2_backend.dto.users.UserResponse;
import com.skillstorm.fincen_project2_backend.exceptions.ResourceConflictException;
import com.skillstorm.fincen_project2_backend.exceptions.ResourceNotFoundException;
import com.skillstorm.fincen_project2_backend.mappers.UserMapper;
import com.skillstorm.fincen_project2_backend.models.User;
import com.skillstorm.fincen_project2_backend.repositories.UserRepository;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository repo;
    private final UserMapper mapper;

    public UserService(@NonNull UserRepository repo, @NonNull UserMapper mapper) {
        this.repo = Objects.requireNonNull(repo, "repo must not be null");
        this.mapper = Objects.requireNonNull(mapper, "mapper must not be null");
    }

    @Transactional
    public UserResponse create(@NonNull CreateUserRequest req) {
        // Normalize upfront (and keep behavior consistent with CITEXT)
        String email = normalizeRequired(req.email(), "Email is required.");

        if (repo.existsByEmail(email)) {
            throw new ResourceConflictException("Email is already in use.");
        }

        User user = Objects.requireNonNull(mapper.toEntity(req), "mapper.toEntity returned null");

        try {
            User saved = Objects.requireNonNull(repo.save(user), "repo.save returned null");
            UUID id = Objects.requireNonNull(saved.getUserId(), "Saved userId must not be null");

            User savedWithRoles = Objects.requireNonNull(
                    repo.findWithRolesByUserId(id).orElse(saved),
                    "savedWithRoles must not be null");

            return mapper.toResponse(savedWithRoles);

        } catch (DataIntegrityViolationException e) {
            throw new ResourceConflictException("Email is already in use.");
        }
    }

    @Transactional(readOnly = true)
    public UserResponse getById(@NonNull UUID userId) {
        UUID id = Objects.requireNonNull(userId, "userId must not be null");

        User user = Objects.requireNonNull(
                repo.findWithRolesByUserId(id)
                        .orElseThrow(() -> new ResourceNotFoundException("User not found.")),
                "user must not be null");

        return mapper.toResponse(user);
    }

    @Transactional(readOnly = true)
    @NonNull
    public User getEntityById(@NonNull UUID userId) {
        UUID id = Objects.requireNonNull(userId, "userId must not be null");

        return Objects.requireNonNull(
                repo.findById(id)
                        .orElseThrow(() -> new ResourceNotFoundException("User not found.")),
                "user must not be null");
    }

    @Transactional(readOnly = true)
    @NonNull
    public User getEntityByEmail(@NonNull String email) {
        String normalized = normalizeRequired(email, "Email is required.");

        User user = Objects.requireNonNull(
                repo.findByEmail(normalized)
                        .orElseThrow(() -> new ResourceNotFoundException("User not found.")),
                "user must not be null");

        return user;
    }

    @Transactional
    public UserResponse updateProfile(@NonNull UUID userId, UpdateUserRequest req) {
        UUID id = Objects.requireNonNull(userId, "userId must not be null");

        User user = Objects.requireNonNull(
                repo.findById(id)
                        .orElseThrow(() -> new ResourceNotFoundException("User not found.")),
                "user must not be null");

        mapper.applyUpdate(req, user);

        User saved = Objects.requireNonNull(repo.save(user), "repo.save returned null");
        UUID savedId = Objects.requireNonNull(saved.getUserId(), "Saved userId must not be null");

        User savedWithRoles = Objects.requireNonNull(
                repo.findWithRolesByUserId(savedId).orElse(saved),
                "savedWithRoles must not be null");

        return mapper.toResponse(savedWithRoles);
    }

    @Transactional
    public UserResponse updateStatus(@NonNull UUID userId, UpdateUserStatusRequest req) {
        UUID id = Objects.requireNonNull(userId, "userId must not be null");

        User user = Objects.requireNonNull(
                repo.findById(id)
                        .orElseThrow(() -> new ResourceNotFoundException("User not found.")),
                "user must not be null");

        mapper.applyUpdate(req, user);

        User saved = Objects.requireNonNull(repo.save(user), "repo.save returned null");
        UUID savedId = Objects.requireNonNull(saved.getUserId(), "Saved userId must not be null");

        User savedWithRoles = Objects.requireNonNull(
                repo.findWithRolesByUserId(savedId).orElse(saved),
                "savedWithRoles must not be null");

        return mapper.toResponse(savedWithRoles);
    }

    @Transactional
    public void delete(@NonNull UUID userId) {
        UUID id = Objects.requireNonNull(userId, "userId must not be null");

        if (!repo.existsById(id)) {
            throw new ResourceNotFoundException("User not found.");
        }

        repo.deleteById(id);
    }

    private static String normalizeRequired(String value, String message) {
        if (value == null) {
            throw new IllegalArgumentException(message);
        }
        String trimmed = value.trim();
        if (trimmed.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return trimmed;
    }
}
