package com.skillstorm.reserveone.mappers;

import org.springframework.stereotype.Component;

import com.skillstorm.reserveone.dto.RoomTypeRequestDTO;
import com.skillstorm.reserveone.dto.RoomTypeResponseDTO;
import com.skillstorm.reserveone.models.Hotel;
import com.skillstorm.reserveone.models.RoomType;

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

