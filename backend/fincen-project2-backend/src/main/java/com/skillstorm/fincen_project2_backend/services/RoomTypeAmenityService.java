package com.skillstorm.fincen_project2_backend.services;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.skillstorm.fincen_project2_backend.dtos.RoomTypeAmenityRequestDTO;
import com.skillstorm.fincen_project2_backend.dtos.RoomTypeAmenityResponseDTO;
import com.skillstorm.fincen_project2_backend.mappers.RoomTypeAmenityMapper;
import com.skillstorm.fincen_project2_backend.models.Amenity;
import com.skillstorm.fincen_project2_backend.models.RoomType;
import com.skillstorm.fincen_project2_backend.models.RoomTypeAmenity;
import com.skillstorm.fincen_project2_backend.models.RoomTypeAmenityId;
import com.skillstorm.fincen_project2_backend.repositories.AmenityRepository;
import com.skillstorm.fincen_project2_backend.repositories.RoomTypeAmenityRepository;
import com.skillstorm.fincen_project2_backend.repositories.RoomTypeRepository;

import exceptions.ResourceNotFoundException;

@Service
@Transactional
public class RoomTypeAmenityService {

    private final RoomTypeAmenityRepository roomTypeAmenityRepository;
    private final RoomTypeRepository roomTypeRepository;
    private final AmenityRepository amenityRepository;
    private final RoomTypeAmenityMapper mapper;

    public RoomTypeAmenityService(
            RoomTypeAmenityRepository roomTypeAmenityRepository,
            RoomTypeRepository roomTypeRepository,
            AmenityRepository amenityRepository,
            RoomTypeAmenityMapper mapper) {
        this.roomTypeAmenityRepository = roomTypeAmenityRepository;
        this.roomTypeRepository = roomTypeRepository;
        this.amenityRepository = amenityRepository;
        this.mapper = mapper;
    }

    public RoomTypeAmenityResponseDTO createOne(RoomTypeAmenityRequestDTO dto) {
        RoomType roomType = roomTypeRepository.findById(dto.roomTypeId())
            .orElseThrow(() -> new ResourceNotFoundException("RoomType not found with id: " + dto.roomTypeId()));
        
        Amenity amenity = amenityRepository.findById(dto.amenityId())
            .orElseThrow(() -> new ResourceNotFoundException("Amenity not found with id: " + dto.amenityId()));
        
        RoomTypeAmenity roomTypeAmenity = mapper.toEntity(dto, roomType, amenity);
        RoomTypeAmenity saved = roomTypeAmenityRepository.save(roomTypeAmenity);
        return mapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public RoomTypeAmenityResponseDTO readOne(UUID roomTypeId, UUID amenityId) {
        RoomTypeAmenityId id = new RoomTypeAmenityId(roomTypeId, amenityId);
        RoomTypeAmenity roomTypeAmenity = roomTypeAmenityRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException(
                "RoomTypeAmenity not found with roomTypeId: " + roomTypeId + " and amenityId: " + amenityId));
        return mapper.toResponse(roomTypeAmenity);
    }

    @Transactional(readOnly = true)
    public List<RoomTypeAmenityResponseDTO> readAll() {
        return roomTypeAmenityRepository.findAll().stream()
            .map(mapper::toResponse)
            .collect(Collectors.toList());
    }

    public RoomTypeAmenityResponseDTO updateOne(UUID roomTypeId, UUID amenityId, RoomTypeAmenityRequestDTO dto) {
        RoomTypeAmenityId id = new RoomTypeAmenityId(roomTypeId, amenityId);
        RoomTypeAmenity existing = roomTypeAmenityRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException(
                "RoomTypeAmenity not found with roomTypeId: " + roomTypeId + " and amenityId: " + amenityId));
        
        // Delete old association
        roomTypeAmenityRepository.delete(existing);
        
        // Create new association
        RoomType roomType = roomTypeRepository.findById(dto.roomTypeId())
            .orElseThrow(() -> new ResourceNotFoundException("RoomType not found with id: " + dto.roomTypeId()));
        
        Amenity amenity = amenityRepository.findById(dto.amenityId())
            .orElseThrow(() -> new ResourceNotFoundException("Amenity not found with id: " + dto.amenityId()));
        
        RoomTypeAmenity roomTypeAmenity = mapper.toEntity(dto, roomType, amenity);
        RoomTypeAmenity saved = roomTypeAmenityRepository.save(roomTypeAmenity);
        return mapper.toResponse(saved);
    }

    public void deleteOne(UUID roomTypeId, UUID amenityId) {
        RoomTypeAmenityId id = new RoomTypeAmenityId(roomTypeId, amenityId);
        if (!roomTypeAmenityRepository.existsById(id)) {
            throw new ResourceNotFoundException(
                "RoomTypeAmenity not found with roomTypeId: " + roomTypeId + " and amenityId: " + amenityId);
        }
        roomTypeAmenityRepository.deleteById(id);
    }
}

