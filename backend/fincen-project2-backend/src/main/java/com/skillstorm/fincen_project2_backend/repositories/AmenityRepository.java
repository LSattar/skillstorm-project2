package com.skillstorm.fincen_project2_backend.repositories;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.skillstorm.fincen_project2_backend.models.Amenity;

@Repository
public interface AmenityRepository extends JpaRepository<Amenity, UUID> {
}

