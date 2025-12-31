package com.skillstorm.fincen_project2_backend.services;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.skillstorm.fincen_project2_backend.dtos.ReservationRequestDTO;
import com.skillstorm.fincen_project2_backend.dtos.ReservationResponseDTO;
import com.skillstorm.fincen_project2_backend.mappers.ReservationMapper;
import com.skillstorm.fincen_project2_backend.models.Hotel;
import com.skillstorm.fincen_project2_backend.models.Reservation;
import com.skillstorm.fincen_project2_backend.models.Reservation.Status;
import com.skillstorm.fincen_project2_backend.models.Room;
import com.skillstorm.fincen_project2_backend.models.RoomType;
import com.skillstorm.fincen_project2_backend.models.User;
import com.skillstorm.fincen_project2_backend.repositories.HotelRepository;
import com.skillstorm.fincen_project2_backend.repositories.ReservationRepository;
import com.skillstorm.fincen_project2_backend.repositories.RoomRepository;
import com.skillstorm.fincen_project2_backend.repositories.RoomTypeRepository;
import com.skillstorm.fincen_project2_backend.repositories.UserRepository;

import exceptions.ResourceConflictException;
import exceptions.ResourceNotFoundException;

@Service
@Transactional
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final HotelRepository hotelRepository;
    private final RoomRepository roomRepository;
    private final RoomTypeRepository roomTypeRepository;
    private final ReservationMapper mapper;

    private final UserRepository userRepository;

    public ReservationService(
            ReservationRepository reservationRepository,
            HotelRepository hotelRepository,
            RoomRepository roomRepository,
            RoomTypeRepository roomTypeRepository,
            ReservationMapper mapper,
            UserRepository userRepository) {
        this.reservationRepository = reservationRepository;
        this.hotelRepository = hotelRepository;
        this.roomRepository = roomRepository;
        this.roomTypeRepository = roomTypeRepository;
        this.mapper = mapper;
        this.userRepository = userRepository;
    }

    public ReservationResponseDTO createOne(ReservationRequestDTO dto) {
        // Validate dates
        if (dto.endDate().isBefore(dto.startDate()) || dto.endDate().equals(dto.startDate())) {
            throw new IllegalArgumentException("End date must be after start date");
        }

        if (dto.startDate().isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Start date cannot be in the past");
        }

        // Fetch related entities
        Hotel hotel = hotelRepository.findById(dto.hotelId())
            .orElseThrow(() -> new ResourceNotFoundException("Hotel not found with id: " + dto.hotelId()));

        User user = userRepository.findById(dto.userId())
            .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + dto.userId()));

        Room room = roomRepository.findById(dto.roomId())
            .orElseThrow(() -> new ResourceNotFoundException("Room not found with id: " + dto.roomId()));

        RoomType roomType = roomTypeRepository.findById(dto.roomTypeId())
            .orElseThrow(() -> new ResourceNotFoundException("RoomType not found with id: " + dto.roomTypeId()));

        // Check for overlapping reservations (active statuses)
        List<Status> activeStatuses = List.of(Status.PENDING, Status.CONFIRMED, Status.CHECKED_IN);
        List<Reservation> overlapping = reservationRepository
            .findByRoom_RoomIdAndStatusInAndStartDateLessThanAndEndDateGreaterThan(
                dto.roomId(), activeStatuses, dto.endDate(), dto.startDate());

        if (!overlapping.isEmpty()) {
            throw new ResourceConflictException(
                "Room is already reserved for the selected date range");
        }

        // Validate guest count against room type capacity
        if (dto.guestCount() > roomType.getMaxGuests()) {
            throw new IllegalArgumentException(
                "Guest count (" + dto.guestCount() + ") exceeds room capacity (" + roomType.getMaxGuests() + ")");
        }

        Reservation reservation = mapper.toEntity(dto, hotel, user, room, roomType);
        Reservation saved = reservationRepository.save(reservation);
        return mapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public ReservationResponseDTO readOne(UUID id) {
        Reservation reservation = reservationRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Reservation not found with id: " + id));
        return mapper.toResponse(reservation);
    }

    @Transactional(readOnly = true)
    public List<ReservationResponseDTO> readAll() {
        return reservationRepository.findAll().stream()
            .map(mapper::toResponse)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ReservationResponseDTO> readByUserId(UUID userId) {
        return reservationRepository.findByUser_UserId(userId).stream()
            .map(mapper::toResponse)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ReservationResponseDTO> readByHotelId(UUID hotelId) {
        return reservationRepository.findByHotel_HotelId(hotelId).stream()
            .map(mapper::toResponse)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ReservationResponseDTO> readByRoomId(UUID roomId) {
        return reservationRepository.findByRoom_RoomId(roomId).stream()
            .map(mapper::toResponse)
            .collect(Collectors.toList());
    }

    public ReservationResponseDTO updateOne(UUID id, ReservationRequestDTO dto) {
        Reservation reservation = reservationRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Reservation not found with id: " + id));

        // Validate dates
        if (dto.endDate().isBefore(dto.startDate()) || dto.endDate().equals(dto.startDate())) {
            throw new IllegalArgumentException("End date must be after start date");
        }

        // Fetch related entities
        Hotel hotel = hotelRepository.findById(dto.hotelId())
            .orElseThrow(() -> new ResourceNotFoundException("Hotel not found with id: " + dto.hotelId()));

        User user = userRepository.findById(dto.userId())
            .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + dto.userId()));

        Room room = roomRepository.findById(dto.roomId())
            .orElseThrow(() -> new ResourceNotFoundException("Room not found with id: " + dto.roomId()));

        RoomType roomType = roomTypeRepository.findById(dto.roomTypeId())
            .orElseThrow(() -> new ResourceNotFoundException("RoomType not found with id: " + dto.roomTypeId()));

        // Check for overlapping reservations (excluding current reservation)
        List<Status> activeStatuses = List.of(Status.PENDING, Status.CONFIRMED, Status.CHECKED_IN);
        List<Reservation> overlapping = reservationRepository
            .findByRoom_RoomIdAndStatusInAndStartDateLessThanAndEndDateGreaterThan(
                dto.roomId(), activeStatuses, dto.endDate(), dto.startDate());

        overlapping.removeIf(r -> r.getReservationId().equals(id));
        if (!overlapping.isEmpty()) {
            throw new ResourceConflictException(
                "Room is already reserved for the selected date range");
        }

        // Validate guest count
        if (dto.guestCount() > roomType.getMaxGuests()) {
            throw new IllegalArgumentException(
                "Guest count (" + dto.guestCount() + ") exceeds room capacity (" + roomType.getMaxGuests() + ")");
        }

        mapper.applyUpdate(dto, reservation, hotel, user, room, roomType);
        Reservation updated = reservationRepository.save(reservation);
        return mapper.toResponse(updated);
    }

    public void deleteOne(UUID id) {
        if (!reservationRepository.existsById(id)) {
            throw new ResourceNotFoundException("Reservation not found with id: " + id);
        }
        reservationRepository.deleteById(id);
    }

    public ReservationResponseDTO cancelReservation(UUID id, String reason) {
        Reservation reservation = reservationRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Reservation not found with id: " + id));

        if (reservation.getStatus() == Status.CANCELLED) {
            throw new ResourceConflictException("Reservation is already cancelled");
        }

        if (reservation.getStatus() == Status.CHECKED_OUT) {
            throw new ResourceConflictException("Cannot cancel a checked-out reservation");
        }

        reservation.setStatus(Status.CANCELLED);
        if (reason != null && !reason.isBlank()) {
            reservation.setCancellationReason(reason.trim());
        }
        reservation.setCancelledAt(java.time.OffsetDateTime.now());

        Reservation updated = reservationRepository.save(reservation);
        return mapper.toResponse(updated);
    }
}

