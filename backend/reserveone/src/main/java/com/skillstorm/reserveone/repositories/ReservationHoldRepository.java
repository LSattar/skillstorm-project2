package com.skillstorm.reserveone.repositories;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.skillstorm.reserveone.models.ReservationHold;
import com.skillstorm.reserveone.models.ReservationHold.Status;

@Repository
public interface ReservationHoldRepository extends JpaRepository<ReservationHold, UUID> {
    
    List<ReservationHold> findByHotel_HotelId(UUID hotelId);
    
    List<ReservationHold> findByUser_UserId(UUID userId);
    
    List<ReservationHold> findByRoom_RoomId(UUID roomId);
    
    List<ReservationHold> findByStatus(Status status);
    
    List<ReservationHold> findByStatusAndExpiresAtBefore(Status status, OffsetDateTime expiresAt);
    
    // Check for overlapping holds (for a given room and date range)
    List<ReservationHold> findByRoom_RoomIdAndStatusAndStartDateLessThanAndEndDateGreaterThan(
        UUID roomId, Status status, LocalDate endDate, LocalDate startDate);
}

