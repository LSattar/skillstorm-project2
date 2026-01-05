package com.skillstorm.reserveone.repositories;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.skillstorm.reserveone.models.Amenity;

@Repository
public interface AmenityRepository extends JpaRepository<Amenity, UUID> {
}

