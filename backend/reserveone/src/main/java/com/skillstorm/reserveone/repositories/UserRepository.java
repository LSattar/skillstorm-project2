package com.skillstorm.reserveone.repositories;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.lang.NonNull;

import com.skillstorm.reserveone.models.User;

public interface UserRepository extends JpaRepository<User, UUID> {

    boolean existsByEmail(@NonNull String email);

    Optional<User> findByEmail(@NonNull String email);

    @EntityGraph(attributePaths = { "roles" })
    Optional<User> findWithRolesByEmail(@NonNull String email);

    @EntityGraph(attributePaths = { "roles" })
    Optional<User> findWithRolesByUserId(@NonNull UUID userId);

    boolean existsByUserId(@NonNull UUID userId);
}
