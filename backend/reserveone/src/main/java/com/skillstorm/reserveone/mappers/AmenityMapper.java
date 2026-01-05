package com.skillstorm.reserveone.mappers;

import org.springframework.stereotype.Component;

import com.skillstorm.reserveone.dto.AmenityRequestDTO;
import com.skillstorm.reserveone.dto.AmenityResponseDTO;
import com.skillstorm.reserveone.models.Amenity;

@Component
public class AmenityMapper {

    // CREATE
    public Amenity toEntity(AmenityRequestDTO req) {
        if (req == null) {
            return null;
        }

        Amenity amenity = new Amenity(req.name().trim(), req.category());

        if (req.isActive() != null) {
            amenity.setIsActive(req.isActive());
        }

        return amenity;
    }

    // READ
    public AmenityResponseDTO toResponse(Amenity amenity) {
        if (amenity == null) {
            return null;
        }

        return new AmenityResponseDTO(
                amenity.getAmenityId(),
                amenity.getName(),
                amenity.getCategory(),
                amenity.getIsActive());
    }

    // UPDATE
    public void applyUpdate(AmenityRequestDTO req, Amenity amenity) {
        if (req == null || amenity == null) {
            return;
        }

        amenity.setName(req.name().trim());
        amenity.setCategory(req.category());

        if (req.isActive() != null) {
            amenity.setIsActive(req.isActive());
        }
    }
}

