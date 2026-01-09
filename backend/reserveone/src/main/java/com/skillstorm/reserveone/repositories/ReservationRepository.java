package com.skillstorm.reserveone.repositories;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.skillstorm.reserveone.models.Reservation;
import com.skillstorm.reserveone.models.Reservation.Status;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, UUID> {
    
    List<Reservation> findByHotel_HotelId(UUID hotelId);
    
    List<Reservation> findByUser_UserId(UUID userId);
    
    List<Reservation> findByRoom_RoomId(UUID roomId);
    
    List<Reservation> findByRoomType_RoomTypeId(UUID roomTypeId);
    
    List<Reservation> findByStatus(Status status);
    
    List<Reservation> findByHotel_HotelIdAndStatus(UUID hotelId, Status status);
    
    List<Reservation> findByUser_UserIdAndStatus(UUID userId, Status status);
    
    List<Reservation> findByRoom_RoomIdAndStatusIn(UUID roomId, List<Status> statuses);
    
    // Check for overlapping reservations (for a given room and date range)
    // Using half-open range [startDate, endDate)
    List<Reservation> findByRoom_RoomIdAndStatusInAndStartDateLessThanAndEndDateGreaterThan(
        UUID roomId, List<Status> statuses, LocalDate endDate, LocalDate startDate);
    
    // Find all reservations for a list of rooms in a date range
    @Query("SELECT r FROM Reservation r WHERE r.room.roomId IN :roomIds AND " +
           "r.status IN :statuses AND " +
           "r.startDate < :endDate AND r.endDate > :startDate")
    List<Reservation> findByRoomIdsAndStatusInAndDateRange(
        @Param("roomIds") List<UUID> roomIds,
        @Param("statuses") List<Status> statuses,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate);
}

