package com.skillstorm.reserveone.services;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.skillstorm.reserveone.dto.ReservationHoldRequestDTO;
import com.skillstorm.reserveone.dto.ReservationHoldResponseDTO;
import com.skillstorm.reserveone.mappers.ReservationHoldMapper;
import com.skillstorm.reserveone.models.Hotel;
import com.skillstorm.reserveone.models.ReservationHold;
import com.skillstorm.reserveone.models.ReservationHold.Status;
import com.skillstorm.reserveone.models.Room;
import com.skillstorm.reserveone.models.User;
import com.skillstorm.reserveone.repositories.HotelRepository;
import com.skillstorm.reserveone.repositories.ReservationHoldRepository;
import com.skillstorm.reserveone.repositories.RoomRepository;
import com.skillstorm.reserveone.repositories.UserRepository;

import com.skillstorm.reserveone.exceptions.ResourceConflictException;
import com.skillstorm.reserveone.exceptions.ResourceNotFoundException;

@Service
@Transactional
public class ReservationHoldService {

    private final ReservationHoldRepository holdRepository;
    private final HotelRepository hotelRepository;
    private final RoomRepository roomRepository;
    private final UserRepository userRepository;
    private final ReservationHoldMapper mapper;

    public ReservationHoldService(
            ReservationHoldRepository holdRepository,
            HotelRepository hotelRepository,
            RoomRepository roomRepository,
            UserRepository userRepository,
            ReservationHoldMapper mapper) {
        this.holdRepository = holdRepository;
        this.hotelRepository = hotelRepository;
        this.roomRepository = roomRepository;
        this.userRepository = userRepository;
        this.mapper = mapper;
    }

    public ReservationHoldResponseDTO createOne(ReservationHoldRequestDTO dto) {
        // Validate dates
        if (dto.endDate().isBefore(dto.startDate()) || dto.endDate().equals(dto.startDate())) {
            throw new IllegalArgumentException("End date must be after start date");
        }

        if (dto.expiresAt().isBefore(OffsetDateTime.now())) {
            throw new IllegalArgumentException("Expires at cannot be in the past");
        }

        // Fetch related entities
        Hotel hotel = hotelRepository.findById(dto.hotelId())
            .orElseThrow(() -> new ResourceNotFoundException("Hotel not found with id: " + dto.hotelId()));

        Room room = roomRepository.findById(dto.roomId())
            .orElseThrow(() -> new ResourceNotFoundException("Room not found with id: " + dto.roomId()));

        User user = userRepository.findById(dto.userId())
            .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + dto.userId()));

        // Check for overlapping active holds
        List<ReservationHold> overlapping = holdRepository
            .findByRoom_RoomIdAndStatusAndStartDateLessThanAndEndDateGreaterThan(
                dto.roomId(), Status.ACTIVE, dto.endDate(), dto.startDate());

        if (!overlapping.isEmpty()) {
            throw new ResourceConflictException(
                "Room has an active hold for the selected date range");
        }

        ReservationHold hold = mapper.toEntity(dto, hotel, room, user);
        ReservationHold saved = holdRepository.save(hold);
        return mapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public ReservationHoldResponseDTO readOne(UUID id) {
        ReservationHold hold = holdRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Reservation hold not found with id: " + id));
        return mapper.toResponse(hold);
    }

    @Transactional(readOnly = true)
    public List<ReservationHoldResponseDTO> readAll() {
        return holdRepository.findAll().stream()
            .map(mapper::toResponse)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ReservationHoldResponseDTO> readByUserId(UUID userId) {
        return holdRepository.findByUser_UserId(userId).stream()
            .map(mapper::toResponse)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ReservationHoldResponseDTO> readByRoomId(UUID roomId) {
        return holdRepository.findByRoom_RoomId(roomId).stream()
            .map(mapper::toResponse)
            .collect(Collectors.toList());
    }

    public ReservationHoldResponseDTO updateOne(UUID id, ReservationHoldRequestDTO dto) {
        ReservationHold hold = holdRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Reservation hold not found with id: " + id));

        // Validate dates
        if (dto.endDate().isBefore(dto.startDate()) || dto.endDate().equals(dto.startDate())) {
            throw new IllegalArgumentException("End date must be after start date");
        }

        if (dto.expiresAt().isBefore(OffsetDateTime.now())) {
            throw new IllegalArgumentException("Expires at cannot be in the past");
        }

        // Fetch related entities
        Hotel hotel = hotelRepository.findById(dto.hotelId())
            .orElseThrow(() -> new ResourceNotFoundException("Hotel not found with id: " + dto.hotelId()));

        Room room = roomRepository.findById(dto.roomId())
            .orElseThrow(() -> new ResourceNotFoundException("Room not found with id: " + dto.roomId()));

        User user = userRepository.findById(dto.userId())
            .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + dto.userId()));

        // Check for overlapping active holds (excluding current hold)
        List<ReservationHold> overlapping = holdRepository
            .findByRoom_RoomIdAndStatusAndStartDateLessThanAndEndDateGreaterThan(
                dto.roomId(), Status.ACTIVE, dto.endDate(), dto.startDate());

        overlapping.removeIf(h -> h.getHoldId().equals(id));
        if (!overlapping.isEmpty()) {
            throw new ResourceConflictException(
                "Room has an active hold for the selected date range");
        }

        mapper.applyUpdate(dto, hold, hotel, room, user);
        ReservationHold updated = holdRepository.save(hold);
        return mapper.toResponse(updated);
    }

    public void deleteOne(UUID id) {
        if (!holdRepository.existsById(id)) {
            throw new ResourceNotFoundException("Reservation hold not found with id: " + id);
        }
        holdRepository.deleteById(id);
    }

    public ReservationHoldResponseDTO cancelHold(UUID id) {
        ReservationHold hold = holdRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Reservation hold not found with id: " + id));

        if (hold.getStatus() == Status.CANCELLED) {
            throw new ResourceConflictException("Hold is already cancelled");
        }

        if (hold.getStatus() == Status.EXPIRED) {
            throw new ResourceConflictException("Cannot cancel an expired hold");
        }

        hold.setStatus(Status.CANCELLED);
        ReservationHold updated = holdRepository.save(hold);
        return mapper.toResponse(updated);
    }

    @Transactional
    public void expireHolds() {
        List<ReservationHold> expiredHolds = holdRepository
            .findByStatusAndExpiresAtBefore(Status.ACTIVE, OffsetDateTime.now());

        for (ReservationHold hold : expiredHolds) {
            hold.setStatus(Status.EXPIRED);
        }
        holdRepository.saveAll(expiredHolds);
    }
}

