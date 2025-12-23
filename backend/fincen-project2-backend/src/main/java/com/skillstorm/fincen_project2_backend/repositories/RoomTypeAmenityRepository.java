package com.skillstorm.fincen_project2_backend.repositories;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.skillstorm.fincen_project2_backend.models.RoomTypeAmenity;
import com.skillstorm.fincen_project2_backend.models.RoomTypeAmenityId;

@Repository
public interface RoomTypeAmenityRepository extends JpaRepository<RoomTypeAmenity, RoomTypeAmenityId> {
    
    List<RoomTypeAmenity> findByRoomType_RoomTypeId(UUID roomTypeId);
    
    List<RoomTypeAmenity> findByAmenity_AmenityId(UUID amenityId);
    
    void deleteByRoomType_RoomTypeIdAndAmenity_AmenityId(UUID roomTypeId, UUID amenityId);
}

