package com.skillstorm.fincen_project2_backend.mappers;

import org.springframework.stereotype.Component;

import com.skillstorm.fincen_project2_backend.dtos.ReservationHoldRequestDTO;
import com.skillstorm.fincen_project2_backend.dtos.ReservationHoldResponseDTO;
import com.skillstorm.fincen_project2_backend.models.Hotel;
import com.skillstorm.fincen_project2_backend.models.ReservationHold;
import com.skillstorm.fincen_project2_backend.models.Room;
import com.skillstorm.fincen_project2_backend.models.User;

@Component
public class ReservationHoldMapper {

    // CREATE
    public ReservationHold toEntity(ReservationHoldRequestDTO req, Hotel hotel, Room room, User user) {
        if (req == null || hotel == null || room == null || user == null) {
            return null;
        }

        return new ReservationHold(hotel, room, user, req.startDate(), req.endDate(), req.expiresAt());
    }

    // READ
    public ReservationHoldResponseDTO toResponse(ReservationHold hold) {
        if (hold == null) {
            return null;
        }

        return new ReservationHoldResponseDTO(
                hold.getHoldId(),
                hold.getHotel().getHotelId(),
                hold.getRoom().getRoomId(),
                hold.getUser().getUserId(),
                hold.getStartDate(),
                hold.getEndDate(),
                hold.getStatus(),
                hold.getExpiresAt(),
                hold.getCreatedAt(),
                hold.getUpdatedAt());
    }

    // UPDATE
    public void applyUpdate(ReservationHoldRequestDTO req, ReservationHold hold, 
                           Hotel hotel, Room room, User user) {
        if (req == null || hold == null || hotel == null || room == null || user == null) {
            return;
        }

        hold.setHotel(hotel);
        hold.setRoom(room);
        hold.setUser(user);
        hold.setStartDate(req.startDate());
        hold.setEndDate(req.endDate());
        hold.setExpiresAt(req.expiresAt());
    }
}

