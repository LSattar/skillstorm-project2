package com.skillstorm.reserveone.services;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.skillstorm.reserveone.dto.roles.CreateRoleRequest;
import com.skillstorm.reserveone.dto.roles.RoleResponse;
import com.skillstorm.reserveone.exceptions.ResourceConflictException;
import com.skillstorm.reserveone.exceptions.ResourceNotFoundException;
import com.skillstorm.reserveone.mappers.RoleMapper;
import com.skillstorm.reserveone.models.Role;
import com.skillstorm.reserveone.repositories.RoleRepository;

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

        String normalizedName = normalizeRoleName(req.name(), "Role name is required.");

        // Optional fail-fast (still keep DB constraint as authority)
        if (repo.existsByName(normalizedName)) {
            throw new ResourceConflictException("Role name is already in use.");
        }

        Role role = Objects.requireNonNull(mapper.toEntity(normalizedName), "mapper.toEntity returned null");

        try {
            Role saved = Objects.requireNonNull(repo.save(role), "repo.save returned null");
            return Objects.requireNonNull(mapper.toResponse(saved), "mapper.toResponse returned null");
        } catch (DataIntegrityViolationException e) {
            // In case of race / concurrent create
            throw new ResourceConflictException("Role name is already in use.");
        }
    }

    @Transactional(readOnly = true)
    public RoleResponse getById(@NonNull UUID roleId) {
        UUID id = Objects.requireNonNull(roleId, "roleId must not be null");

        Role role = repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found."));

        return Objects.requireNonNull(mapper.toResponse(role), "mapper.toResponse returned null");
    }

    @Transactional(readOnly = true)
    public Role getEntityByName(@NonNull String name) {
        String normalizedName = normalizeRoleName(name, "Role name is required.");

        return repo.findByName(normalizedName)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found."));
    }

    @Transactional(readOnly = true)
    public List<RoleResponse> getAll() {
        return repo.findAll().stream()
                .sorted(Comparator.comparing(Role::getName, Comparator.nullsLast(String::compareTo)))
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

    @NonNull
    private static String normalizeRoleName(String value, String message) {
        if (value == null) {
            throw new IllegalArgumentException(message);
        }

        String trimmed = value.trim();
        if (trimmed.isBlank()) {
            throw new IllegalArgumentException(message);
        }

        return Objects.requireNonNull(trimmed.toUpperCase(Locale.ROOT));
    }
}
