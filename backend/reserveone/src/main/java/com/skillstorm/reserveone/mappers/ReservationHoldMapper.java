package com.skillstorm.reserveone.mappers;

import org.springframework.stereotype.Component;

import com.skillstorm.reserveone.dto.ReservationHoldRequestDTO;
import com.skillstorm.reserveone.dto.ReservationHoldResponseDTO;
import com.skillstorm.reserveone.models.Hotel;
import com.skillstorm.reserveone.models.ReservationHold;
import com.skillstorm.reserveone.models.Room;
import com.skillstorm.reserveone.models.User;

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

