package com.skillstorm.reserveone.repositories;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.skillstorm.reserveone.models.Hotel;

@Repository
public interface HotelRepository extends JpaRepository<Hotel, UUID> {
    
    @Query("SELECT h FROM Hotel h WHERE " +
           "(:city IS NULL OR LOWER(h.city) LIKE LOWER(CONCAT('%', :city, '%'))) AND " +
           "(:state IS NULL OR LOWER(h.state) = LOWER(:state))")
    List<Hotel> findByLocation(@Param("city") String city, @Param("state") String state);
}

