package com.skillstorm.reserveone.mappers;

import org.springframework.stereotype.Component;

import com.skillstorm.reserveone.dto.RoomTypeAmenityRequestDTO;
import com.skillstorm.reserveone.dto.RoomTypeAmenityResponseDTO;
import com.skillstorm.reserveone.models.Amenity;
import com.skillstorm.reserveone.models.RoomType;
import com.skillstorm.reserveone.models.RoomTypeAmenity;

@Component
public class RoomTypeAmenityMapper {

    // CREATE
    public RoomTypeAmenity toEntity(RoomTypeAmenityRequestDTO req, RoomType roomType, Amenity amenity) {
        if (req == null || roomType == null || amenity == null) {
            return null;
        }

        return new RoomTypeAmenity(roomType, amenity);
    }

    // READ
    public RoomTypeAmenityResponseDTO toResponse(RoomTypeAmenity roomTypeAmenity) {
        if (roomTypeAmenity == null) {
            return null;
        }

        return new RoomTypeAmenityResponseDTO(
                roomTypeAmenity.getRoomTypeId(),
                roomTypeAmenity.getAmenityId());
    }
}

