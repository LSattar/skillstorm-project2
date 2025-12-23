package com.skillstorm.fincen_project2_backend.services;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.skillstorm.fincen_project2_backend.dtos.AmenityRequestDTO;
import com.skillstorm.fincen_project2_backend.dtos.AmenityResponseDTO;
import com.skillstorm.fincen_project2_backend.mappers.AmenityMapper;
import com.skillstorm.fincen_project2_backend.models.Amenity;
import com.skillstorm.fincen_project2_backend.repositories.AmenityRepository;

import exceptions.ResourceNotFoundException;

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

