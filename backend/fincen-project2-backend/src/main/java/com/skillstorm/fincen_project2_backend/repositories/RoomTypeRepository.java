package com.skillstorm.fincen_project2_backend.repositories;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.skillstorm.fincen_project2_backend.models.RoomType;

@Repository
public interface RoomTypeRepository extends JpaRepository<RoomType, UUID> {
    
    List<RoomType> findByHotel_HotelId(UUID hotelId);
    
    List<RoomType> findByHotel_HotelIdAndIsActiveTrue(UUID hotelId);
}

