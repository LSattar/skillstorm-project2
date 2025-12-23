package com.skillstorm.fincen_project2_backend.repositories;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.skillstorm.fincen_project2_backend.models.User;

public interface UserRepository extends JpaRepository<User, UUID> {

    // Fail-fast check for duplicate emails (maps to ResourceConflictException)
    boolean existsByEmail(String email);

    // Used for lookups / auth flows
    Optional<User> findByEmail(String email);

    // Fetch user with roles eagerly when needed for responses
    @EntityGraph(attributePaths = "roles")
    Optional<User> findWithRolesByUserId(UUID userId);
}
