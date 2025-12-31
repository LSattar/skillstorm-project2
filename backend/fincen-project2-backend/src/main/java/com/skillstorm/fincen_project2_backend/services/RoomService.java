package com.skillstorm.fincen_project2_backend.services;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.skillstorm.fincen_project2_backend.dtos.RoomRequestDTO;
import com.skillstorm.fincen_project2_backend.dtos.RoomResponseDTO;
import com.skillstorm.fincen_project2_backend.dtos.RoomSearchDTO;
import com.skillstorm.fincen_project2_backend.mappers.RoomMapper;
import com.skillstorm.fincen_project2_backend.repositories.RoomTypeAmenityRepository;
import com.skillstorm.fincen_project2_backend.models.Hotel;
import com.skillstorm.fincen_project2_backend.models.Reservation;
import com.skillstorm.fincen_project2_backend.models.Reservation.Status;
import com.skillstorm.fincen_project2_backend.models.ReservationHold;
import com.skillstorm.fincen_project2_backend.models.Room;
import com.skillstorm.fincen_project2_backend.models.RoomType;
import com.skillstorm.fincen_project2_backend.repositories.HotelRepository;
import com.skillstorm.fincen_project2_backend.repositories.ReservationHoldRepository;
import com.skillstorm.fincen_project2_backend.repositories.ReservationRepository;
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
    private final ReservationRepository reservationRepository;
    private final ReservationHoldRepository holdRepository;
    private final RoomTypeAmenityRepository roomTypeAmenityRepository;
    private final RoomMapper mapper;

    public RoomService(
            RoomRepository roomRepository,
            HotelRepository hotelRepository,
            RoomTypeRepository roomTypeRepository,
            ReservationRepository reservationRepository,
            ReservationHoldRepository holdRepository,
            RoomTypeAmenityRepository roomTypeAmenityRepository,
            RoomMapper mapper) {
        this.roomRepository = roomRepository;
        this.hotelRepository = hotelRepository;
        this.roomTypeRepository = roomTypeRepository;
        this.reservationRepository = reservationRepository;
        this.holdRepository = holdRepository;
        this.roomTypeAmenityRepository = roomTypeAmenityRepository;
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
    public List<RoomResponseDTO> searchAvailableRooms(RoomSearchDTO search) {
        // Validate dates
        if (search.endDate().isBefore(search.startDate()) || search.endDate().equals(search.startDate())) {
            throw new IllegalArgumentException("End date must be after start date");
        }

        if (search.startDate().isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Start date cannot be in the past");
        }

        // Calculate number of nights for price calculations
        long nights = java.time.temporal.ChronoUnit.DAYS.between(search.startDate(), search.endDate());

        // Start with all rooms, then filter
        List<Room> candidateRooms;

        // Filter by hotel if provided
        if (search.hotelId() != null) {
            candidateRooms = roomRepository.findByHotel_HotelId(search.hotelId());
        } else {
            candidateRooms = roomRepository.findAll();
        }

        // Filter by room type if provided
        if (search.roomTypeId() != null) {
            candidateRooms = candidateRooms.stream()
                .filter(room -> room.getRoomType().getRoomTypeId().equals(search.roomTypeId()))
                .collect(Collectors.toList());
        }

        // Filter to only available rooms (status = AVAILABLE)
        candidateRooms = candidateRooms.stream()
            .filter(room -> room.getStatus() == Room.Status.AVAILABLE)
            .collect(Collectors.toList());

        // Filter by room type properties
        candidateRooms = candidateRooms.stream()
            .filter(room -> {
                RoomType roomType = room.getRoomType();
                
                // Filter by active status
                if (search.activeOnly() && !roomType.getIsActive()) {
                    return false;
                }
                
                // Filter by guest capacity
                if (search.minGuests() != null && roomType.getMaxGuests() < search.minGuests()) {
                    return false;
                }
                
                // Filter by bed type
                if (search.bedType() != null && roomType.getBedType() != search.bedType()) {
                    return false;
                }
                
                // Filter by bed count
                if (search.minBedCount() != null && roomType.getBedCount() < search.minBedCount()) {
                    return false;
                }
                
                // Filter by price per night
                BigDecimal pricePerNight = roomType.getBasePrice();
                if (search.minPricePerNight() != null && pricePerNight.compareTo(search.minPricePerNight()) < 0) {
                    return false;
                }
                if (search.maxPricePerNight() != null && pricePerNight.compareTo(search.maxPricePerNight()) > 0) {
                    return false;
                }
                
                // Filter by total price
                if (search.maxTotalPrice() != null) {
                    BigDecimal totalPrice = pricePerNight.multiply(BigDecimal.valueOf(nights));
                    if (totalPrice.compareTo(search.maxTotalPrice()) > 0) {
                        return false;
                    }
                }
                
                // Filter by required amenities
                if (search.requiredAmenityIds() != null && !search.requiredAmenityIds().isEmpty()) {
                    Set<UUID> roomTypeAmenityIds = roomTypeAmenityRepository
                        .findByRoomType_RoomTypeId(roomType.getRoomTypeId())
                        .stream()
                        .map(rta -> rta.getAmenity().getAmenityId())
                        .collect(Collectors.toSet());
                    
                    if (!roomTypeAmenityIds.containsAll(search.requiredAmenityIds())) {
                        return false;
                    }
                }
                
                return true;
            })
            .collect(Collectors.toList());

        // Check availability for each room
        List<Status> activeReservationStatuses = List.of(Status.PENDING, Status.CONFIRMED, Status.CHECKED_IN);
        
        List<Room> availableRooms = candidateRooms.stream()
            .filter(room -> {
                UUID roomId = room.getRoomId();
                
                // Check for overlapping reservations
                List<Reservation> overlappingReservations = reservationRepository
                    .findByRoom_RoomIdAndStatusInAndStartDateLessThanAndEndDateGreaterThan(
                        roomId, activeReservationStatuses, search.endDate(), search.startDate());
                
                if (!overlappingReservations.isEmpty()) {
                    return false;
                }
                
                // Check for overlapping active holds
                List<ReservationHold> overlappingHolds = holdRepository
                    .findByRoom_RoomIdAndStatusAndStartDateLessThanAndEndDateGreaterThan(
                        roomId, ReservationHold.Status.ACTIVE, search.endDate(), search.startDate());
                
                return overlappingHolds.isEmpty();
            })
            .collect(Collectors.toList());

        return availableRooms.stream()
            .map(mapper::toResponse)
            .collect(Collectors.toList());
    }
}

