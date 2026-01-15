package com.skillstorm.reserveone.services;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.skillstorm.reserveone.dto.RoomTypeRequestDTO;
import com.skillstorm.reserveone.dto.RoomTypeResponseDTO;
import com.skillstorm.reserveone.mappers.RoomTypeMapper;
import com.skillstorm.reserveone.models.Hotel;
import com.skillstorm.reserveone.models.RoomType;
import com.skillstorm.reserveone.repositories.HotelRepository;
import com.skillstorm.reserveone.repositories.RoomTypeRepository;

import com.skillstorm.reserveone.exceptions.ResourceNotFoundException;

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

    @Transactional(readOnly = true)
    public List<RoomTypeResponseDTO> readByHotelId(UUID hotelId) {
        return roomTypeRepository.findByHotel_HotelId(hotelId).stream()
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

