package com.skillstorm.reserveone.services;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.skillstorm.reserveone.dto.OperationalMetricsDTO;
import com.skillstorm.reserveone.models.Reservation;
import com.skillstorm.reserveone.models.Reservation.Status;
import com.skillstorm.reserveone.models.Room;
import com.skillstorm.reserveone.repositories.ReservationRepository;
import com.skillstorm.reserveone.repositories.RoomRepository;

@Service
@Transactional(readOnly = true)
public class AdminMetricsService {

    private final RoomRepository roomRepository;
    private final ReservationRepository reservationRepository;

    public AdminMetricsService(
            RoomRepository roomRepository,
            ReservationRepository reservationRepository) {
        this.roomRepository = roomRepository;
        this.reservationRepository = reservationRepository;
    }

    public OperationalMetricsDTO getOperationalMetrics(UUID hotelId) {
        LocalDate today = LocalDate.now();
        
        List<Room> allRooms;
        if (hotelId != null) {
            allRooms = roomRepository.findByHotel_HotelId(hotelId);
        } else {
            allRooms = roomRepository.findAll();
        }
        
        int totalRooms = (int) allRooms.stream()
            .filter(room -> room.getStatus() != Room.Status.OUT_OF_SERVICE)
            .count();
        
        int occupiedRoomsFromStatus = (int) allRooms.stream()
            .filter(room -> room.getStatus() == Room.Status.OCCUPIED)
            .count();
        
        List<Reservation> checkedInReservations;
        if (hotelId != null) {
            checkedInReservations = reservationRepository.findByHotel_HotelIdAndStatus(
                hotelId, Status.CHECKED_IN);
        } else {
            checkedInReservations = reservationRepository.findByStatus(Status.CHECKED_IN);
        }
        
        long distinctOccupiedRooms = checkedInReservations.stream()
            .map(res -> res.getRoom().getRoomId())
            .distinct()
            .count();
        
        int occupiedRooms = Math.max(occupiedRoomsFromStatus, (int) distinctOccupiedRooms);
        
        double occupancyRate = totalRooms > 0 
            ? (occupiedRooms * 100.0 / totalRooms) 
            : 0.0;
        
        List<Reservation> checkInsToday;
        if (hotelId != null) {
            checkInsToday = reservationRepository.findByHotel_HotelIdAndStatusAndStartDate(
                hotelId, Status.CHECKED_IN, today);
        } else {
            checkInsToday = reservationRepository.findByStatusAndStartDate(Status.CHECKED_IN, today);
        }
        
        List<Reservation> checkInsPending;
        if (hotelId != null) {
            checkInsPending = reservationRepository.findByHotel_HotelIdAndStatusAndStartDate(
                hotelId, Status.CONFIRMED, today);
        } else {
            checkInsPending = reservationRepository.findByStatusAndStartDate(Status.CONFIRMED, today);
        }
        
        List<Reservation> checkOutsToday;
        if (hotelId != null) {
            checkOutsToday = reservationRepository.findByHotel_HotelIdAndStatusAndEndDate(
                hotelId, Status.CHECKED_OUT, today);
        } else {
            checkOutsToday = reservationRepository.findByStatusAndEndDate(Status.CHECKED_OUT, today);
        }
        
        List<Reservation> checkOutsPending;
        if (hotelId != null) {
            checkOutsPending = reservationRepository.findByHotel_HotelIdAndStatusAndEndDate(
                hotelId, Status.CHECKED_IN, today);
        } else {
            checkOutsPending = reservationRepository.findByStatusAndEndDate(Status.CHECKED_IN, today);
        }
        
        return new OperationalMetricsDTO(
            totalRooms,
            occupiedRooms,
            Math.round(occupancyRate * 100.0) / 100.0,
            checkInsToday.size(),
            checkInsPending.size(),
            checkOutsToday.size(),
            checkOutsPending.size()
        );
    }
}
