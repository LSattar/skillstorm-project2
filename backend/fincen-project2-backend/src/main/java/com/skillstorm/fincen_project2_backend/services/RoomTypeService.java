package com.skillstorm.fincen_project2_backend.services;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.skillstorm.fincen_project2_backend.dtos.RoomTypeRequestDTO;
import com.skillstorm.fincen_project2_backend.dtos.RoomTypeResponseDTO;
import com.skillstorm.fincen_project2_backend.mappers.RoomTypeMapper;
import com.skillstorm.fincen_project2_backend.models.Hotel;
import com.skillstorm.fincen_project2_backend.models.RoomType;
import com.skillstorm.fincen_project2_backend.repositories.HotelRepository;
import com.skillstorm.fincen_project2_backend.repositories.RoomTypeRepository;

import exceptions.ResourceNotFoundException;

@Service
@Transactional
public class RoomTypeService {

    private final RoomTypeRepository roomTypeRepository;
    private final HotelRepository hotelRepository;
    private final RoomTypeMapper mapper;

    public RoomTypeService(
            RoomTypeRepository roomTypeRepository,
            HotelRepository hotelRepository,
            RoomTypeMapper mapper) {
        this.roomTypeRepository = roomTypeRepository;
        this.hotelRepository = hotelRepository;
        this.mapper = mapper;
    }

    public RoomTypeResponseDTO createOne(RoomTypeRequestDTO dto) {
        Hotel hotel = hotelRepository.findById(dto.hotelId())
            .orElseThrow(() -> new ResourceNotFoundException("Hotel not found with id: " + dto.hotelId()));
        
        RoomType roomType = mapper.toEntity(dto, hotel);
        RoomType saved = roomTypeRepository.save(roomType);
        return mapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public RoomTypeResponseDTO readOne(UUID id) {
        RoomType roomType = roomTypeRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("RoomType not found with id: " + id));
        return mapper.toResponse(roomType);
    }

    @Transactional(readOnly = true)
    public List<RoomTypeResponseDTO> readAll() {
        return roomTypeRepository.findAll().stream()
            .map(mapper::toResponse)
            .collect(Collectors.toList());
    }

    public RoomTypeResponseDTO updateOne(UUID id, RoomTypeRequestDTO dto) {
        RoomType roomType = roomTypeRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("RoomType not found with id: " + id));
        
        Hotel hotel = hotelRepository.findById(dto.hotelId())
            .orElseThrow(() -> new ResourceNotFoundException("Hotel not found with id: " + dto.hotelId()));
        
        mapper.applyUpdate(dto, roomType, hotel);
        RoomType updated = roomTypeRepository.save(roomType);
        return mapper.toResponse(updated);
    }

    public void deleteOne(UUID id) {
        if (!roomTypeRepository.existsById(id)) {
            throw new ResourceNotFoundException("RoomType not found with id: " + id);
        }
        roomTypeRepository.deleteById(id);
    }
}

