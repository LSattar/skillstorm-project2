package com.skillstorm.fincen_project2_backend.services;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.skillstorm.fincen_project2_backend.dtos.RoomRequestDTO;
import com.skillstorm.fincen_project2_backend.dtos.RoomResponseDTO;
import com.skillstorm.fincen_project2_backend.mappers.RoomMapper;
import com.skillstorm.fincen_project2_backend.models.Hotel;
import com.skillstorm.fincen_project2_backend.models.Room;
import com.skillstorm.fincen_project2_backend.models.RoomType;
import com.skillstorm.fincen_project2_backend.repositories.HotelRepository;
import com.skillstorm.fincen_project2_backend.repositories.RoomRepository;
import com.skillstorm.fincen_project2_backend.repositories.RoomTypeRepository;

import exceptions.ResourceConflictException;
import exceptions.ResourceNotFoundException;

@Service
@Transactional
public class RoomService {

    private final RoomRepository roomRepository;
    private final HotelRepository hotelRepository;
    private final RoomTypeRepository roomTypeRepository;
    private final RoomMapper mapper;

    public RoomService(
            RoomRepository roomRepository,
            HotelRepository hotelRepository,
            RoomTypeRepository roomTypeRepository,
            RoomMapper mapper) {
        this.roomRepository = roomRepository;
        this.hotelRepository = hotelRepository;
        this.roomTypeRepository = roomTypeRepository;
        this.mapper = mapper;
    }

    public RoomResponseDTO createOne(RoomRequestDTO dto) {
        Hotel hotel = hotelRepository.findById(dto.hotelId())
            .orElseThrow(() -> new ResourceNotFoundException("Hotel not found with id: " + dto.hotelId()));
        
        RoomType roomType = roomTypeRepository.findById(dto.roomTypeId())
            .orElseThrow(() -> new ResourceNotFoundException("RoomType not found with id: " + dto.roomTypeId()));
        
        // Check for duplicate room number within the same hotel
        if (roomRepository.existsByHotel_HotelIdAndRoomNumber(dto.hotelId(), dto.roomNumber())) {
            throw new ResourceConflictException(
                "Room with number " + dto.roomNumber() + " already exists for hotel " + dto.hotelId());
        }
        
        Room room = mapper.toEntity(dto, hotel, roomType);
        Room saved = roomRepository.save(room);
        return mapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public RoomResponseDTO readOne(UUID id) {
        Room room = roomRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Room not found with id: " + id));
        return mapper.toResponse(room);
    }

    @Transactional(readOnly = true)
    public List<RoomResponseDTO> readAll() {
        return roomRepository.findAll().stream()
            .map(mapper::toResponse)
            .collect(Collectors.toList());
    }

    public RoomResponseDTO updateOne(UUID id, RoomRequestDTO dto) {
        Room room = roomRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Room not found with id: " + id));
        
        Hotel hotel = hotelRepository.findById(dto.hotelId())
            .orElseThrow(() -> new ResourceNotFoundException("Hotel not found with id: " + dto.hotelId()));
        
        RoomType roomType = roomTypeRepository.findById(dto.roomTypeId())
            .orElseThrow(() -> new ResourceNotFoundException("RoomType not found with id: " + dto.roomTypeId()));
        
        // Check for duplicate room number if room number is being changed
        if (!room.getRoomNumber().equals(dto.roomNumber()) &&
            roomRepository.existsByHotel_HotelIdAndRoomNumber(dto.hotelId(), dto.roomNumber())) {
            throw new ResourceConflictException(
                "Room with number " + dto.roomNumber() + " already exists for hotel " + dto.hotelId());
        }
        
        mapper.applyUpdate(dto, room, hotel, roomType);
        Room updated = roomRepository.save(room);
        return mapper.toResponse(updated);
    }

    public void deleteOne(UUID id) {
        if (!roomRepository.existsById(id)) {
            throw new ResourceNotFoundException("Room not found with id: " + id);
        }
        roomRepository.deleteById(id);
    }
}

