package com.skillstorm.fincen_project2_backend.services;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

import com.skillstorm.fincen_project2_backend.dto.roles.CreateRoleRequest;
import com.skillstorm.fincen_project2_backend.dto.roles.RoleResponse;
import com.skillstorm.fincen_project2_backend.exceptions.ResourceConflictException;
import com.skillstorm.fincen_project2_backend.exceptions.ResourceNotFoundException;
import com.skillstorm.fincen_project2_backend.mappers.RoleMapper;
import com.skillstorm.fincen_project2_backend.models.Role;
import com.skillstorm.fincen_project2_backend.repositories.RoleRepository;

import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RoleService {

    private final RoleRepository repo;
    private final RoleMapper mapper;

    public RoleService(@NonNull RoleRepository repo, @NonNull RoleMapper mapper) {
        this.repo = Objects.requireNonNull(repo, "repo must not be null");
        this.mapper = Objects.requireNonNull(mapper, "mapper must not be null");
    }

    @Transactional
    public RoleResponse create(@NonNull CreateRoleRequest req) {
        Objects.requireNonNull(req, "req must not be null");

        // enforce UPPERCASE at the service boundary (and again in entity constructor)
        String normalizedName = normalizeRoleName(req.name(), "Role name is required.");

        // fail-fast for cleaner 409 message than DB constraint, when possible
        if (repo.existsByName(normalizedName)) {
            throw new ResourceConflictException("Role name is already in use.");
        }

        // IMPORTANT: let DataIntegrityViolationException bubble to
        // GlobalExceptionHandler
        // so it returns your standardized ProblemDetail message.
        Role role = Objects.requireNonNull(
                mapper.toEntity(normalizedName),
                "mapper.toEntity returned null");

        Role saved = Objects.requireNonNull(repo.save(role), "repo.save returned null");
        return Objects.requireNonNull(mapper.toResponse(saved), "mapper.toResponse returned null");
    }

    @Transactional(readOnly = true)
    public RoleResponse getById(@NonNull UUID roleId) {
        UUID id = Objects.requireNonNull(roleId, "roleId must not be null");

        Role role = Objects.requireNonNull(
                repo.findById(id).orElseThrow(() -> new ResourceNotFoundException("Role not found.")),
                "role must not be null");

        return Objects.requireNonNull(mapper.toResponse(role), "mapper.toResponse returned null");
    }

    @Transactional(readOnly = true)
    public Role getEntityByName(@NonNull String name) {
        String normalizedName = normalizeRoleName(name, "Role name is required.");

        Role role = Objects.requireNonNull(
                repo.findByName(normalizedName)
                        .orElseThrow(() -> new ResourceNotFoundException("Role not found.")),
                "role must not be null");

        return role;
    }

    @Transactional(readOnly = true)
    public List<RoleResponse> getAll() {
        // findAll() will not return null elements; mapper only returns null for null
        // input
        return repo.findAll().stream()
                .map(mapper::toResponse)
                .toList();
    }

    @Transactional
    public void delete(@NonNull UUID roleId) {
        UUID id = Objects.requireNonNull(roleId, "roleId must not be null");

        if (!repo.existsById(id)) {
            throw new ResourceNotFoundException("Role not found.");
        }

        repo.deleteById(id);
    }

    private static String normalizeRoleName(String value, String message) {
        if (value == null) {
            throw new IllegalArgumentException(message);
        }
        String trimmed = value.trim();
        if (trimmed.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return trimmed.toUpperCase();
    }
}
