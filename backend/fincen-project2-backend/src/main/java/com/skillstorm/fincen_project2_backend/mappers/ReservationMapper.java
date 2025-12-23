package com.skillstorm.fincen_project2_backend.mappers;

import org.springframework.stereotype.Component;

import com.skillstorm.fincen_project2_backend.dtos.ReservationRequestDTO;
import com.skillstorm.fincen_project2_backend.dtos.ReservationResponseDTO;
import com.skillstorm.fincen_project2_backend.models.Hotel;
import com.skillstorm.fincen_project2_backend.models.Reservation;
import com.skillstorm.fincen_project2_backend.models.Room;
import com.skillstorm.fincen_project2_backend.models.RoomType;
import com.skillstorm.fincen_project2_backend.models.User;

@Component
public class ReservationMapper {

    // CREATE
    public Reservation toEntity(ReservationRequestDTO req, Hotel hotel, User user, Room room, RoomType roomType) {
        if (req == null || hotel == null || user == null || room == null || roomType == null) {
            return null;
        }

        Reservation reservation = new Reservation(hotel, user, room, roomType, 
                req.startDate(), req.endDate(), req.guestCount());

        if (req.status() != null) {
            reservation.setStatus(req.status());
        }

        if (req.totalAmount() != null) {
            reservation.setTotalAmount(req.totalAmount());
        }

        if (req.currency() != null && !req.currency().isBlank()) {
            reservation.setCurrency(req.currency().trim().toUpperCase());
        }

        if (req.specialRequests() != null && !req.specialRequests().isBlank()) {
            reservation.setSpecialRequests(req.specialRequests().trim());
        }

        return reservation;
    }

    // READ
    public ReservationResponseDTO toResponse(Reservation reservation) {
        if (reservation == null) {
            return null;
        }

        return new ReservationResponseDTO(
                reservation.getReservationId(),
                reservation.getHotel().getHotelId(),
                reservation.getUser().getUserId(),
                reservation.getRoom().getRoomId(),
                reservation.getRoomType().getRoomTypeId(),
                reservation.getStartDate(),
                reservation.getEndDate(),
                reservation.getGuestCount(),
                reservation.getStatus(),
                reservation.getTotalAmount(),
                reservation.getCurrency(),
                reservation.getSpecialRequests(),
                reservation.getCancellationReason(),
                reservation.getCancelledAt(),
                reservation.getCancelledByUser() != null ? reservation.getCancelledByUser().getUserId() : null,
                reservation.getCreatedAt(),
                reservation.getUpdatedAt());
    }

    // UPDATE
    public void applyUpdate(ReservationRequestDTO req, Reservation reservation, 
                           Hotel hotel, User user, Room room, RoomType roomType) {
        if (req == null || reservation == null || hotel == null || 
            user == null || room == null || roomType == null) {
            return;
        }

        reservation.setHotel(hotel);
        reservation.setUser(user);
        reservation.setRoom(room);
        reservation.setRoomType(roomType);
        reservation.setStartDate(req.startDate());
        reservation.setEndDate(req.endDate());
        reservation.setGuestCount(req.guestCount());

        if (req.status() != null) {
            reservation.setStatus(req.status());
        }

        if (req.totalAmount() != null) {
            reservation.setTotalAmount(req.totalAmount());
        }

        if (req.currency() != null && !req.currency().isBlank()) {
            reservation.setCurrency(req.currency().trim().toUpperCase());
        }

        if (req.specialRequests() != null) {
            reservation.setSpecialRequests(req.specialRequests() != null && !req.specialRequests().isBlank() 
                ? req.specialRequests().trim() : null);
        }
    }
}

