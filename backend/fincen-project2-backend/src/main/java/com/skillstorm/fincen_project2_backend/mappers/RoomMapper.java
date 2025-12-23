package com.skillstorm.fincen_project2_backend.mappers;

import org.springframework.stereotype.Component;

import com.skillstorm.fincen_project2_backend.dtos.RoomRequestDTO;
import com.skillstorm.fincen_project2_backend.dtos.RoomResponseDTO;
import com.skillstorm.fincen_project2_backend.models.Hotel;
import com.skillstorm.fincen_project2_backend.models.Room;
import com.skillstorm.fincen_project2_backend.models.RoomType;

@Component
public class RoomMapper {

    // CREATE
    public Room toEntity(RoomRequestDTO req, Hotel hotel, RoomType roomType) {
        if (req == null || hotel == null || roomType == null) {
            return null;
        }

        Room room = new Room(hotel, roomType, req.roomNumber());

        if (req.floor() != null && !req.floor().isBlank()) {
            room.setFloor(req.floor().trim());
        }

        if (req.status() != null) {
            room.setStatus(req.status());
        }

        if (req.notes() != null && !req.notes().isBlank()) {
            room.setNotes(req.notes().trim());
        }

        return room;
    }

    // READ
    public RoomResponseDTO toResponse(Room room) {
        if (room == null) {
            return null;
        }

        return new RoomResponseDTO(
                room.getRoomId(),
                room.getHotel().getHotelId(),
                room.getRoomType().getRoomTypeId(),
                room.getRoomNumber(),
                room.getFloor(),
                room.getStatus(),
                room.getNotes(),
                room.getCreatedAt(),
                room.getUpdatedAt());
    }

    // UPDATE
    public void applyUpdate(RoomRequestDTO req, Room room, Hotel hotel, RoomType roomType) {
        if (req == null || room == null || hotel == null || roomType == null) {
            return;
        }

        room.setHotel(hotel);
        room.setRoomType(roomType);
        room.setRoomNumber(req.roomNumber());

        if (req.floor() != null) {
            room.setFloor(req.floor());
        }

        if (req.status() != null) {
            room.setStatus(req.status());
        }

        if (req.notes() != null) {
            room.setNotes(req.notes());
        }
    }
}

