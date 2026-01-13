package com.skillstorm.reserveone.services;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.skillstorm.reserveone.dto.users.CreateUserRequest;
import com.skillstorm.reserveone.dto.users.UpdateUserRequest;
import com.skillstorm.reserveone.dto.users.UpdateUserRolesRequest;
import com.skillstorm.reserveone.dto.users.UpdateUserStatusRequest;
import com.skillstorm.reserveone.dto.users.UserResponse;
import com.skillstorm.reserveone.exceptions.ResourceConflictException;
import com.skillstorm.reserveone.exceptions.ResourceNotFoundException;
import com.skillstorm.reserveone.mappers.UserMapper;
import com.skillstorm.reserveone.models.Role;
import com.skillstorm.reserveone.models.User;
import com.skillstorm.reserveone.repositories.RoleRepository;
import com.skillstorm.reserveone.repositories.UserRepository;

@Service
public class UserService {
    @Transactional
    public void deactivateUser(UUID userId, UUID actingAdminId) {
        if (userId.equals(actingAdminId)) {
            throw new ResourceConflictException("You cannot deactivate your own account.");
        }
        User user = repo.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found."));
        user.setStatus(User.Status.INACTIVE);
        repo.save(user);
    }

    @Transactional
    public void activateUser(UUID userId, UUID actingAdminId) {
        User user = repo.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found."));
        user.setStatus(User.Status.ACTIVE);
        repo.save(user);
    }

    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<com.skillstorm.reserveone.dto.users.UserSearchResponse> searchAdminUsers(
            String q, String status, org.springframework.data.domain.Pageable pageable) {
        org.springframework.data.domain.Page<User> users = repo.searchAdminUsers(q, status, pageable);
        return users.map(u -> new com.skillstorm.reserveone.dto.users.UserSearchResponse(
                u.getUserId(), u.getFirstName(), u.getLastName(), u.getEmail(), u.getStatus()));
    }

    private final UserRepository repo;
    private final UserMapper mapper;
    private final RoleRepository roleRepo;

    public UserService(@NonNull UserRepository repo, @NonNull UserMapper mapper,
            @NonNull RoleRepository roleRepo) {
        this.repo = Objects.requireNonNull(repo, "repo must not be null");
        this.mapper = Objects.requireNonNull(mapper, "mapper must not be null");
        this.roleRepo = Objects.requireNonNull(roleRepo, "roleRepo must not be null");
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

    @Transactional(readOnly = true)
    public List<UserResponse> search(@NonNull String q, int limit) {
        final String query = q.trim();
        final int size = Math.min(Math.max(limit, 1), 50);

        return repo.searchAdminUsers(query, "ACTIVE", PageRequest.of(0, size))
                .stream()
                .map(mapper::toResponse)
                .toList();
    }

    @Transactional
    public UserResponse updateRoles(@NonNull UUID userId, @NonNull UpdateUserRolesRequest req) {
        Objects.requireNonNull(req, "req must not be null");
        final UUID id = Objects.requireNonNull(userId, "userId must not be null");

        final User user = repo.findWithRolesByUserId(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found."));

        applyRoleChanges(user, req.add(), req.remove());

        final User saved = Objects.requireNonNull(repo.save(user), "repo.save returned null");
        final UUID savedId = Objects.requireNonNull(saved.getUserId(), "saved.userId must not be null");

        final User withRoles = repo.findWithRolesByUserId(savedId)
                .orElseThrow(() -> new IllegalStateException(
                        "User updated but could not be reloaded with roles: " + savedId));

        return Objects.requireNonNull(mapper.toResponse(withRoles), "mapper.toResponse returned null");
    }

    private void applyRoleChanges(User user, Set<String> add, Set<String> remove) {
        if (remove != null) {
            for (String raw : remove) {
                String roleName = normalizeRoleNameOptional(raw);
                if (roleName == null)
                    continue;

                Role role = roleRepo.findByName(roleName)
                        .orElseThrow(() -> new ResourceNotFoundException("Role not found: " + roleName));

                user.removeRole(role);
            }
        }

        if (add != null) {
            for (String raw : add) {
                String roleName = normalizeRoleNameOptional(raw);
                if (roleName == null)
                    continue;

                Role role = roleRepo.findByName(roleName)
                        .orElseThrow(() -> new ResourceNotFoundException("Role not found: " + roleName));

                user.addRole(role);
            }
        }
    }

    private static String normalizeRoleNameOptional(String value) {
        if (value == null)
            return null;
        String t = value.trim();
        if (t.isBlank())
            return null;
        return t.toUpperCase(Locale.ROOT);
    }

}
