package com.skillstorm.fincen_project2_backend.mappers;

import org.springframework.stereotype.Component;

import com.skillstorm.fincen_project2_backend.dtos.RoomTypeRequestDTO;
import com.skillstorm.fincen_project2_backend.dtos.RoomTypeResponseDTO;
import com.skillstorm.fincen_project2_backend.models.Hotel;
import com.skillstorm.fincen_project2_backend.models.RoomType;

@Component
public class RoomTypeMapper {

    // CREATE
    public RoomType toEntity(RoomTypeRequestDTO req, Hotel hotel) {
        if (req == null || hotel == null) {
            return null;
        }

        RoomType roomType = new RoomType(hotel, req.name().trim(), req.basePrice(), req.maxGuests());

        if (req.description() != null && !req.description().isBlank()) {
            roomType.setDescription(req.description().trim());
        }

        if (req.bedCount() != null) {
            roomType.setBedCount(req.bedCount());
        }

        if (req.bedType() != null) {
            roomType.setBedType(req.bedType());
        }

        if (req.isActive() != null) {
            roomType.setIsActive(req.isActive());
        }

        return roomType;
    }

    // READ
    public RoomTypeResponseDTO toResponse(RoomType roomType) {
        if (roomType == null) {
            return null;
        }

        return new RoomTypeResponseDTO(
                roomType.getRoomTypeId(),
                roomType.getHotel().getHotelId(),
                roomType.getName(),
                roomType.getDescription(),
                roomType.getBasePrice(),
                roomType.getMaxGuests(),
                roomType.getBedCount(),
                roomType.getBedType(),
                roomType.getIsActive(),
                roomType.getCreatedAt(),
                roomType.getUpdatedAt());
    }

    // UPDATE
    public void applyUpdate(RoomTypeRequestDTO req, RoomType roomType, Hotel hotel) {
        if (req == null || roomType == null || hotel == null) {
            return;
        }

        roomType.setHotel(hotel);
        roomType.setName(req.name().trim());

        if (req.description() != null) {
            roomType.setDescription(req.description().trim());
        }

        roomType.setBasePrice(req.basePrice());
        roomType.setMaxGuests(req.maxGuests());

        if (req.bedCount() != null) {
            roomType.setBedCount(req.bedCount());
        }

        if (req.bedType() != null) {
            roomType.setBedType(req.bedType());
        }

        if (req.isActive() != null) {
            roomType.setIsActive(req.isActive());
        }
    }
}

