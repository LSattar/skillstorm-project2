package com.skillstorm.reserveone.services;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.skillstorm.reserveone.dto.HotelRequestDTO;
import com.skillstorm.reserveone.dto.HotelResponseDTO;
import com.skillstorm.reserveone.mappers.HotelMapper;
import com.skillstorm.reserveone.models.Hotel;
import com.skillstorm.reserveone.repositories.HotelRepository;

import com.skillstorm.reserveone.exceptions.ResourceNotFoundException;

@Service
@Transactional
public class HotelService {

    private final HotelRepository hotelRepository;
    private final HotelMapper mapper;

    public HotelService(HotelRepository hotelRepository, HotelMapper mapper) {
        this.hotelRepository = hotelRepository;
        this.mapper = mapper;
    }

    public HotelResponseDTO createOne(HotelRequestDTO dto) {
        Hotel hotel = mapper.toEntity(dto);
        Hotel saved = hotelRepository.save(hotel);
        return mapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public HotelResponseDTO readOne(UUID id) {
        Hotel hotel = hotelRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Hotel not found with id: " + id));
        return mapper.toResponse(hotel);
    }

    @Transactional(readOnly = true)
    public List<HotelResponseDTO> readAll() {
        return hotelRepository.findAll().stream()
            .map(mapper::toResponse)
            .collect(Collectors.toList());
    }

    public HotelResponseDTO updateOne(UUID id, HotelRequestDTO dto) {
        Hotel hotel = hotelRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Hotel not found with id: " + id));
        
        mapper.applyUpdate(dto, hotel);
        Hotel updated = hotelRepository.save(hotel);
        return mapper.toResponse(updated);
    }

    public void deleteOne(UUID id) {
        if (!hotelRepository.existsById(id)) {
            throw new ResourceNotFoundException("Hotel not found with id: " + id);
        }
        hotelRepository.deleteById(id);
    }
}

