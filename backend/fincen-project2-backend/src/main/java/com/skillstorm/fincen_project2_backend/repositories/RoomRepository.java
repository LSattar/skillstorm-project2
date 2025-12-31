package com.skillstorm.fincen_project2_backend.repositories;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.skillstorm.fincen_project2_backend.models.Room;
import com.skillstorm.fincen_project2_backend.models.Room.Status;

@Repository
public interface RoomRepository extends JpaRepository<Room, UUID> {
    
    List<Room> findByHotel_HotelId(UUID hotelId);
    
    List<Room> findByRoomType_RoomTypeId(UUID roomTypeId);
    
    List<Room> findByHotel_HotelIdAndStatus(UUID hotelId, Status status);
    
    boolean existsByHotel_HotelIdAndRoomNumber(UUID hotelId, String roomNumber);
}

