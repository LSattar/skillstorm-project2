package com.skillstorm.fincen_project2_backend.repositories;

import java.util.UUID;

import com.skillstorm.fincen_project2_backend.models.Role;

import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleRepository extends JpaRepository<Role, UUID> {

}
