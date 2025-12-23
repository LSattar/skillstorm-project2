package com.skillstorm.fincen_project2_backend.mappers;

import org.springframework.stereotype.Component;

import com.skillstorm.fincen_project2_backend.dtos.RoomTypeAmenityRequestDTO;
import com.skillstorm.fincen_project2_backend.dtos.RoomTypeAmenityResponseDTO;
import com.skillstorm.fincen_project2_backend.models.Amenity;
import com.skillstorm.fincen_project2_backend.models.RoomType;
import com.skillstorm.fincen_project2_backend.models.RoomTypeAmenity;

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

