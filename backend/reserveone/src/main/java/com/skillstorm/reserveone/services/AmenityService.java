package com.skillstorm.reserveone.services;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.skillstorm.reserveone.dto.AmenityRequestDTO;
import com.skillstorm.reserveone.dto.AmenityResponseDTO;
import com.skillstorm.reserveone.mappers.AmenityMapper;
import com.skillstorm.reserveone.models.Amenity;
import com.skillstorm.reserveone.repositories.AmenityRepository;

import com.skillstorm.reserveone.exceptions.ResourceNotFoundException;

@Service
@Transactional
public class AmenityService {

    private final AmenityRepository amenityRepository;
    private final AmenityMapper mapper;

    public AmenityService(AmenityRepository amenityRepository, AmenityMapper mapper) {
        this.amenityRepository = amenityRepository;
        this.mapper = mapper;
    }

    public AmenityResponseDTO createOne(AmenityRequestDTO dto) {
        Amenity amenity = mapper.toEntity(dto);
        Amenity saved = amenityRepository.save(amenity);
        return mapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public AmenityResponseDTO readOne(UUID id) {
        Amenity amenity = amenityRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Amenity not found with id: " + id));
        return mapper.toResponse(amenity);
    }

    @Transactional(readOnly = true)
    public List<AmenityResponseDTO> readAll() {
        return amenityRepository.findAll().stream()
            .map(mapper::toResponse)
            .collect(Collectors.toList());
    }

    public AmenityResponseDTO updateOne(UUID id, AmenityRequestDTO dto) {
        Amenity amenity = amenityRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Amenity not found with id: " + id));
        
        mapper.applyUpdate(dto, amenity);
        Amenity updated = amenityRepository.save(amenity);
        return mapper.toResponse(updated);
    }

    public void deleteOne(UUID id) {
        if (!amenityRepository.existsById(id)) {
            throw new ResourceNotFoundException("Amenity not found with id: " + id);
        }
        amenityRepository.deleteById(id);
    }
}

