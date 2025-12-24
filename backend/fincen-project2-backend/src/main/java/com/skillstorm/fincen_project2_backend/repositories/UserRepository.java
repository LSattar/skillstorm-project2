package com.skillstorm.fincen_project2_backend.repositories;

import java.util.Optional;
import java.util.UUID;

import com.skillstorm.fincen_project2_backend.models.User;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, UUID> {

    boolean existsByEmail(String email);

    @EntityGraph(attributePaths = "roles")
    Optional<User> findByEmail(String email);

    @EntityGraph(attributePaths = "roles")
    Optional<User> findWithRolesByUserId(UUID userId);

    Optional<User> findByAuth0Sub(String auth0Sub);
}
