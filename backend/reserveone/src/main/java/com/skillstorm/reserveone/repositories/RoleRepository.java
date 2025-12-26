package com.skillstorm.reserveone.repositories;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.lang.NonNull;

import com.skillstorm.reserveone.models.Role;

public interface RoleRepository extends JpaRepository<Role, UUID> {

    boolean existsByName(@NonNull String name);

    Optional<Role> findByName(@NonNull String name);
}