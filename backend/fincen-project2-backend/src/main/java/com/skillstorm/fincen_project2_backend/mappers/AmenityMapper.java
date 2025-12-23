package com.skillstorm.fincen_project2_backend.mappers;

import org.springframework.stereotype.Component;

import com.skillstorm.fincen_project2_backend.dtos.AmenityRequestDTO;
import com.skillstorm.fincen_project2_backend.dtos.AmenityResponseDTO;
import com.skillstorm.fincen_project2_backend.models.Amenity;

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

