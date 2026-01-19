package com.skillstorm.reserveone.services;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.skillstorm.reserveone.dto.ReservationRequestDTO;
import com.skillstorm.reserveone.dto.ReservationResponseDTO;
import com.skillstorm.reserveone.mappers.ReservationMapper;
import com.skillstorm.reserveone.models.Hotel;
import com.skillstorm.reserveone.models.Reservation;
import com.skillstorm.reserveone.models.Reservation.Status;
import com.skillstorm.reserveone.models.Room;
import com.skillstorm.reserveone.models.RoomType;
import com.skillstorm.reserveone.models.User;
import com.skillstorm.reserveone.repositories.HotelRepository;
import com.skillstorm.reserveone.repositories.ReservationRepository;
import com.skillstorm.reserveone.repositories.RoomRepository;
import com.skillstorm.reserveone.repositories.RoomTypeRepository;
import com.skillstorm.reserveone.repositories.UserRepository;

import com.skillstorm.reserveone.exceptions.ResourceConflictException;
import com.skillstorm.reserveone.exceptions.ResourceNotFoundException;

/**
 * Service for managing hotel reservations, including creation, updates, cancellations,
 * and check-in/check-out operations.
 * 
 * <p>This service handles the complete reservation lifecycle and enforces business rules
 * such as date validation, room availability checks, guest capacity limits, and status
 * transitions. All write operations are transactional to ensure data consistency.
 * 
 * @author ReserveOne Team
 * @since 1.0
 */
@Service
@Transactional
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final HotelRepository hotelRepository;
    private final RoomRepository roomRepository;
    private final RoomTypeRepository roomTypeRepository;
    private final ReservationMapper mapper;
    private final UserRepository userRepository;
    private final EmailService emailService;

    /**
     * Constructs a new ReservationService with the required dependencies.
     * 
     * @param reservationRepository the repository for reservation data access
     * @param hotelRepository the repository for hotel data access
     * @param roomRepository the repository for room data access
     * @param roomTypeRepository the repository for room type data access
     * @param mapper the mapper for converting between DTOs and entities
     * @param userRepository the repository for user data access
     * @param emailService the service for sending email notifications
     */
    public ReservationService(
            ReservationRepository reservationRepository,
            HotelRepository hotelRepository,
            RoomRepository roomRepository,
            RoomTypeRepository roomTypeRepository,
            ReservationMapper mapper,
            UserRepository userRepository,
            EmailService emailService) {
        this.reservationRepository = reservationRepository;
        this.hotelRepository = hotelRepository;
        this.roomRepository = roomRepository;
        this.roomTypeRepository = roomTypeRepository;
        this.mapper = mapper;
        this.userRepository = userRepository;
        this.emailService = emailService;
    }

    /**
     * Creates a new reservation with comprehensive validation and business rule enforcement.
     * 
     * <p>This method performs the following validations and operations:
     * <ol>
     *   <li><b>Date Validation:</b>
     *     <ul>
     *       <li>End date must be after start date</li>
     *       <li>Start date cannot be in the past</li>
     *     </ul>
     *   </li>
     *   <li><b>Entity Validation:</b> Verifies existence of hotel, user, room, and room type</li>
     *   <li><b>Availability Check:</b> Ensures no overlapping reservations exist for the room
     *       with statuses PENDING, CONFIRMED, or CHECKED_IN</li>
     *   <li><b>Capacity Check:</b> Validates guest count does not exceed room type maximum capacity</li>
     *   <li><b>Reservation Creation:</b> Saves the new reservation to the database</li>
     *   <li><b>Email Notification:</b> Sends confirmation email (non-blocking)</li>
     * </ol>
     * 
     * <p><b>Overlap Detection:</b> Uses a half-open interval check [startDate, endDate)
     * to find reservations where:
     * <pre>
     *   reservation.startDate &lt; dto.endDate AND reservation.endDate &gt; dto.startDate
     * </pre>
     * 
     * @param dto the reservation request containing all reservation details
     * @return ReservationResponseDTO representing the created reservation
     * @throws IllegalArgumentException if date validation fails or guest count exceeds capacity
     * @throws ResourceNotFoundException if any referenced entity (hotel, user, room, room type) is not found
     * @throws ResourceConflictException if the room is already reserved for the date range
     */
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
        
        // Send email confirmation (non-blocking - errors are logged but don't fail reservation)
        emailService.sendReservationConfirmation(saved, user);
        
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

    /**
     * Searches reservations using flexible criteria with dynamic query building.
     * 
     * <p>This method supports multi-criteria searching with the following filters:
     * <ul>
     *   <li><b>Reservation ID:</b> Partial match (case-insensitive) on UUID string representation</li>
     *   <li><b>Guest Last Name:</b> Partial match (case-insensitive) on user's last name</li>
     *   <li><b>Hotel ID:</b> Exact match on hotel UUID</li>
     *   <li><b>Status:</b> Exact match on reservation status</li>
     *   <li><b>Start Date Range:</b> Reservations with startDate between startDateFrom and startDateTo (inclusive)</li>
     *   <li><b>End Date Range:</b> Reservations with endDate between endDateFrom and endDateTo (inclusive)</li>
     * </ul>
     * 
     * <p>All filters are optional and combined with AND logic. If a filter parameter is null,
     * it is not applied. The search uses JPA Specifications for type-safe dynamic query building.
     * 
     * @param reservationId partial UUID string to search for (case-insensitive)
     * @param guestLastName partial last name to search for (case-insensitive)
     * @param hotelId exact hotel UUID to filter by
     * @param status exact reservation status to filter by
     * @param startDateFrom minimum start date (inclusive)
     * @param startDateTo maximum start date (inclusive)
     * @param endDateFrom minimum end date (inclusive)
     * @param endDateTo maximum end date (inclusive)
     * @return list of matching reservations as DTOs
     */
    @Transactional(readOnly = true)
    public List<ReservationResponseDTO> searchReservations(
            String reservationId,
            String guestLastName,
            UUID hotelId,
            Status status,
            LocalDate startDateFrom,
            LocalDate startDateTo,
            LocalDate endDateFrom,
            LocalDate endDateTo) {
        
        Specification<Reservation> spec = buildSearchSpecification(
            reservationId, guestLastName, hotelId, status,
            startDateFrom, startDateTo, endDateFrom, endDateTo);
        
        return reservationRepository.findAll(spec).stream()
            .map(mapper::toResponse)
            .collect(Collectors.toList());
    }

    /**
     * Builds a JPA Specification for dynamic reservation queries.
     * 
     * <p>This private helper method constructs a type-safe query specification using
     * JPA Criteria API. It combines multiple optional predicates using AND logic.
     * 
     * <p>The method handles:
     * <ul>
     *   <li>String filtering with LIKE operations (case-insensitive)</li>
     *   <li>UUID equality checks</li>
     *   <li>Enum equality checks</li>
     *   <li>Date range comparisons (greater than or equal, less than or equal)</li>
     * </ul>
     * 
     * @param reservationId partial UUID string filter
     * @param guestLastName partial last name filter
     * @param hotelId hotel UUID filter
     * @param status reservation status filter
     * @param startDateFrom minimum start date
     * @param startDateTo maximum start date
     * @param endDateFrom minimum end date
     * @param endDateTo maximum end date
     * @return JPA Specification combining all non-null filters
     */
    private Specification<Reservation> buildSearchSpecification(
            String reservationId,
            String guestLastName,
            UUID hotelId,
            Status status,
            LocalDate startDateFrom,
            LocalDate startDateTo,
            LocalDate endDateFrom,
            LocalDate endDateTo) {
        
        return (root, query, cb) -> {
            var predicates = cb.conjunction();

            // Filter by reservation ID (partial match on UUID string)
            if (reservationId != null && !reservationId.isBlank()) {
                String searchId = reservationId.trim().toLowerCase();
                predicates = cb.and(predicates, 
                    cb.like(cb.lower(root.get("reservationId").as(String.class)), 
                        "%" + searchId + "%"));
            }

            // Filter by guest last name (from User entity)
            if (guestLastName != null && !guestLastName.isBlank()) {
                String searchLastName = guestLastName.trim().toLowerCase();
                predicates = cb.and(predicates,
                    cb.like(cb.lower(root.get("user").get("lastName")), 
                        "%" + searchLastName + "%"));
            }

            // Filter by hotel ID
            if (hotelId != null) {
                predicates = cb.and(predicates, 
                    cb.equal(root.get("hotel").get("hotelId"), hotelId));
            }

            // Filter by status
            if (status != null) {
                predicates = cb.and(predicates, 
                    cb.equal(root.get("status"), status));
            }

            // Filter by start date range (check-in from)
            if (startDateFrom != null) {
                predicates = cb.and(predicates, 
                    cb.greaterThanOrEqualTo(root.get("startDate"), startDateFrom));
            }

            // Filter by start date range (check-in to)
            if (startDateTo != null) {
                predicates = cb.and(predicates, 
                    cb.lessThanOrEqualTo(root.get("startDate"), startDateTo));
            }

            // Filter by end date range (check-out from)
            if (endDateFrom != null) {
                predicates = cb.and(predicates, 
                    cb.greaterThanOrEqualTo(root.get("endDate"), endDateFrom));
            }

            // Filter by end date range (check-out to)
            if (endDateTo != null) {
                predicates = cb.and(predicates, 
                    cb.lessThanOrEqualTo(root.get("endDate"), endDateTo));
            }

            return predicates;
        };
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

    /**
     * Processes a guest check-in for a confirmed reservation.
     * 
     * <p>This method performs the check-in operation with the following validations:
     * <ul>
     *   <li>Reservation must be in CONFIRMED status</li>
     *   <li>Check-in date cannot be before the reservation start date</li>
     *   <li>Room must not already be marked as OCCUPIED</li>
     * </ul>
     * 
     * <p>Upon successful validation:
     * <ol>
     *   <li>Updates reservation status to CHECKED_IN</li>
     *   <li>Updates room status to OCCUPIED</li>
     *   <li>Persists both changes in a single transaction</li>
     * </ol>
     * 
     * @param id the UUID of the reservation to check in
     * @return ReservationResponseDTO representing the updated reservation
     * @throws ResourceNotFoundException if the reservation is not found
     * @throws ResourceConflictException if reservation is not in CONFIRMED status or room is already occupied
     * @throws IllegalArgumentException if attempting to check in before the reservation start date
     */
    public ReservationResponseDTO checkIn(UUID id) {
        Reservation reservation = reservationRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Reservation not found with id: " + id));

        if (reservation.getStatus() != Status.CONFIRMED) {
            throw new ResourceConflictException(
                "Reservation must be in CONFIRMED status to check in. Current status: " + reservation.getStatus());
        }

        if (reservation.getStartDate().isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Cannot check in before the reservation start date");
        }

        Room room = reservation.getRoom();
        if (room.getStatus() == Room.Status.OCCUPIED) {
            throw new ResourceConflictException("Room is already occupied");
        }

        reservation.setStatus(Status.CHECKED_IN);
        room.setStatus(Room.Status.OCCUPIED);

        Reservation updated = reservationRepository.save(reservation);
        roomRepository.save(room);
        return mapper.toResponse(updated);
    }

    /**
     * Processes a guest check-out for a checked-in reservation.
     * 
     * <p>This method performs the check-out operation with the following validations:
     * <ul>
     *   <li>Reservation must be in CHECKED_IN status</li>
     * </ul>
     * 
     * <p>Upon successful validation:
     * <ol>
     *   <li>Updates reservation status to CHECKED_OUT</li>
     *   <li>Updates room status to AVAILABLE</li>
     *   <li>Persists both changes in a single transaction</li>
     * </ol>
     * 
     * @param id the UUID of the reservation to check out
     * @return ReservationResponseDTO representing the updated reservation
     * @throws ResourceNotFoundException if the reservation is not found
     * @throws ResourceConflictException if reservation is not in CHECKED_IN status
     */
    public ReservationResponseDTO checkOut(UUID id) {
        Reservation reservation = reservationRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Reservation not found with id: " + id));

        if (reservation.getStatus() != Status.CHECKED_IN) {
            throw new ResourceConflictException(
                "Reservation must be in CHECKED_IN status to check out. Current status: " + reservation.getStatus());
        }

        Room room = reservation.getRoom();
        reservation.setStatus(Status.CHECKED_OUT);
        room.setStatus(Room.Status.AVAILABLE);

        Reservation updated = reservationRepository.save(reservation);
        roomRepository.save(room);
        return mapper.toResponse(updated);
    }
}

