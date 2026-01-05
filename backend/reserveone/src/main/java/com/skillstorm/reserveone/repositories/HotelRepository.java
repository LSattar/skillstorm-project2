package com.skillstorm.reserveone.repositories;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.skillstorm.reserveone.models.Hotel;

@Repository
public interface HotelRepository extends JpaRepository<Hotel, UUID> {
}

