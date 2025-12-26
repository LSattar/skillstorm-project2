package com.skillstorm.reserveone.services;

import java.util.Objects;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.skillstorm.reserveone.dto.users.CreateUserRequest;
import com.skillstorm.reserveone.dto.users.UpdateUserRequest;
import com.skillstorm.reserveone.dto.users.UpdateUserStatusRequest;
import com.skillstorm.reserveone.dto.users.UserResponse;
import com.skillstorm.reserveone.exceptions.ResourceConflictException;
import com.skillstorm.reserveone.exceptions.ResourceNotFoundException;
import com.skillstorm.reserveone.mappers.UserMapper;
import com.skillstorm.reserveone.models.User;
import com.skillstorm.reserveone.repositories.UserRepository;

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
        Objects.requireNonNull(req, "req must not be null");

        final String email = normalizeEmailOptional(req.email());
        if (email == null) {
            throw new IllegalArgumentException("Email is required.");
        }

        if (repo.existsByEmail(email)) {
            throw new ResourceConflictException("Email is already in use.");
        }

        final User user = Objects.requireNonNull(mapper.toEntity(req), "mapper.toEntity returned null");
        user.setEmail(email);

        try {
            final User saved = Objects.requireNonNull(repo.save(user), "repo.save returned null");

            final UUID id = Objects.requireNonNull(saved.getUserId(), "Saved userId must not be null");

            // HARD reload (no fallback) to avoid nullness warnings + keep invariant strong
            final User withRoles = Objects.requireNonNull(
                    repo.findWithRolesByUserId(id)
                            .orElseThrow(() -> new IllegalStateException(
                                    "User saved but could not be reloaded with roles: " + id)),
                    "withRoles must not be null");

            return Objects.requireNonNull(mapper.toResponse(withRoles), "mapper.toResponse returned null");

        } catch (DataIntegrityViolationException e) {
            throw new ResourceConflictException("Email is already in use.");
        }
    }

    @Transactional(readOnly = true)
    public UserResponse getById(@NonNull UUID userId) {
        final UUID id = Objects.requireNonNull(userId, "userId must not be null");

        final User withRoles = Objects.requireNonNull(
                repo.findWithRolesByUserId(id)
                        .orElseThrow(() -> new ResourceNotFoundException("User not found.")),
                "withRoles must not be null");

        return Objects.requireNonNull(mapper.toResponse(withRoles), "mapper.toResponse returned null");
    }

    @Transactional(readOnly = true)
    public User getEntityById(@NonNull UUID userId) {
        final UUID id = Objects.requireNonNull(userId, "userId must not be null");

        return Objects.requireNonNull(
                repo.findById(id)
                        .orElseThrow(() -> new ResourceNotFoundException("User not found.")),
                "user must not be null");
    }

    @Transactional(readOnly = true)
    public User getEntityByEmail(@NonNull String email) {
        final String normalized = normalizeEmailRequired(email, "Email is required.");

        return Objects.requireNonNull(
                repo.findWithRolesByEmail(normalized)
                        .orElseThrow(() -> new ResourceNotFoundException("User not found.")),
                "user must not be null");
    }

    @Transactional
    public UserResponse updateProfile(@NonNull UUID userId, UpdateUserRequest req) {
        Objects.requireNonNull(req, "req must not be null");
        final UUID id = Objects.requireNonNull(userId, "userId must not be null");

        final User user = Objects.requireNonNull(
                repo.findById(id)
                        .orElseThrow(() -> new ResourceNotFoundException("User not found.")),
                "user must not be null");

        mapper.applyUpdate(req, user);

        try {
            final User saved = Objects.requireNonNull(repo.save(user), "repo.save returned null");
            final UUID savedId = Objects.requireNonNull(saved.getUserId(), "Saved userId must not be null");

            final User withRoles = Objects.requireNonNull(
                    repo.findWithRolesByUserId(savedId)
                            .orElseThrow(() -> new IllegalStateException(
                                    "User updated but could not be reloaded with roles: " + savedId)),
                    "withRoles must not be null");

            return Objects.requireNonNull(mapper.toResponse(withRoles), "mapper.toResponse returned null");

        } catch (DataIntegrityViolationException e) {
            throw new ResourceConflictException("Update violates a uniqueness constraint.");
        }
    }

    @Transactional
    public UserResponse updateStatus(@NonNull UUID userId, @NonNull UpdateUserStatusRequest req) {
        Objects.requireNonNull(req, "req must not be null");
        final UUID id = Objects.requireNonNull(userId, "userId must not be null");

        final User user = Objects.requireNonNull(
                repo.findById(id)
                        .orElseThrow(() -> new ResourceNotFoundException("User not found.")),
                "user must not be null");

        mapper.applyUpdate(req, user);

        final User saved = Objects.requireNonNull(repo.save(user), "repo.save returned null");
        final UUID savedId = Objects.requireNonNull(saved.getUserId(), "Saved userId must not be null");

        final User withRoles = Objects.requireNonNull(
                repo.findWithRolesByUserId(savedId)
                        .orElseThrow(() -> new IllegalStateException(
                                "User updated but could not be reloaded with roles: " + savedId)),
                "withRoles must not be null");

        return Objects.requireNonNull(mapper.toResponse(withRoles), "mapper.toResponse returned null");
    }

    @Transactional
    public void delete(@NonNull UUID userId) {
        final UUID id = Objects.requireNonNull(userId, "userId must not be null");

        if (!repo.existsById(id)) {
            throw new ResourceNotFoundException("User not found.");
        }

        repo.deleteById(id);
    }

    @NonNull
    private static String normalizeEmailRequired(String value, String message) {
        final String normalized = normalizeEmailOptional(value);
        if (normalized == null) {
            throw new IllegalArgumentException(message);
        }
        return normalized;
    }

    private static String normalizeEmailOptional(String value) {
        if (value == null)
            return null;
        final String trimmed = value.trim();
        if (trimmed.isBlank())
            return null;
        return trimmed.toLowerCase();
    }
}
