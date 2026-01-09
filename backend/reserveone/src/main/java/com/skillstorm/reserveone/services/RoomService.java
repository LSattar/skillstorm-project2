package com.skillstorm.reserveone.services;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.skillstorm.reserveone.dto.RoomRequestDTO;
import com.skillstorm.reserveone.dto.RoomResponseDTO;
import com.skillstorm.reserveone.exceptions.ResourceConflictException;
import com.skillstorm.reserveone.exceptions.ResourceNotFoundException;
import com.skillstorm.reserveone.mappers.RoomMapper;
import com.skillstorm.reserveone.models.Hotel;
import com.skillstorm.reserveone.models.Reservation;
import com.skillstorm.reserveone.models.Room;
import com.skillstorm.reserveone.models.Room.Status;
import com.skillstorm.reserveone.models.RoomType;
import com.skillstorm.reserveone.repositories.HotelRepository;
import com.skillstorm.reserveone.repositories.ReservationRepository;
import com.skillstorm.reserveone.repositories.RoomRepository;
import com.skillstorm.reserveone.repositories.RoomTypeRepository;

@Service
@Transactional
public class RoomService {

    private final RoomRepository roomRepository;
    private final HotelRepository hotelRepository;
    private final RoomTypeRepository roomTypeRepository;
    private final ReservationRepository reservationRepository;
    private final RoomMapper mapper;

    public RoomService(
            RoomRepository roomRepository,
            HotelRepository hotelRepository,
            RoomTypeRepository roomTypeRepository,
            ReservationRepository reservationRepository,
            RoomMapper mapper) {
        this.roomRepository = roomRepository;
        this.hotelRepository = hotelRepository;
        this.roomTypeRepository = roomTypeRepository;
        this.reservationRepository = reservationRepository;
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

    @Transactional(readOnly = true)
    public List<RoomResponseDTO> searchAvailableRooms(
            UUID hotelId,
            LocalDate startDate,
            LocalDate endDate,
            Integer guestCount,
            UUID roomTypeId) {
        
        // Validate dates
        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("Start date and end date are required");
        }
        if (endDate.isBefore(startDate) || endDate.equals(startDate)) {
            throw new IllegalArgumentException("End date must be after start date");
        }
        if (startDate.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Start date cannot be in the past");
        }
        
        // Find rooms in the specified hotel (or all hotels if hotelId is null)
        List<Room> candidateRooms = new ArrayList<>();
        if (hotelId != null) {
            // Validate hotel exists
            if (!hotelRepository.existsById(hotelId)) {
                throw new ResourceNotFoundException("Hotel not found with id: " + hotelId);
            }
            candidateRooms = roomRepository.findByHotel_HotelIdAndStatus(hotelId, Status.AVAILABLE);
        } else {
            // If no hotel specified, search all hotels
            List<Room> allRooms = roomRepository.findAll();
            candidateRooms = allRooms.stream()
                .filter(room -> room.getStatus() == Status.AVAILABLE)
                .collect(Collectors.toList());
        }
        
        // Filter by room type if specified
        if (roomTypeId != null) {
            candidateRooms = candidateRooms.stream()
                .filter(room -> room.getRoomType().getRoomTypeId().equals(roomTypeId))
                .collect(Collectors.toList());
        }
        
        // Filter by guest count (room type must accommodate the number of guests)
        if (guestCount != null && guestCount > 0) {
            candidateRooms = candidateRooms.stream()
                .filter(room -> room.getRoomType().getMaxGuests() >= guestCount)
                .collect(Collectors.toList());
        }
        
        // Filter out rooms with active reservations in the date range
        if (!candidateRooms.isEmpty()) {
            List<UUID> candidateRoomIds = candidateRooms.stream()
                .map(Room::getRoomId)
                .collect(Collectors.toList());
            
            List<Reservation.Status> activeStatuses = List.of(
                Reservation.Status.PENDING,
                Reservation.Status.CONFIRMED,
                Reservation.Status.CHECKED_IN
            );
            
            List<Reservation> overlappingReservations = reservationRepository
                .findByRoomIdsAndStatusInAndDateRange(
                    candidateRoomIds,
                    activeStatuses,
                    startDate,
                    endDate
                );
            
            Set<UUID> reservedRoomIds = overlappingReservations.stream()
                .map(res -> res.getRoom().getRoomId())
                .collect(Collectors.toSet());
            
            candidateRooms = candidateRooms.stream()
                .filter(room -> !reservedRoomIds.contains(room.getRoomId()))
                .collect(Collectors.toList());
        }
        
        // Convert to DTOs
        return candidateRooms.stream()
            .map(mapper::toResponse)
            .collect(Collectors.toList());
    }
}

