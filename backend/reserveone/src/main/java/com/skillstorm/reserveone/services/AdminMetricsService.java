package com.skillstorm.reserveone.services;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.skillstorm.reserveone.dto.DailyOccupancyDTO;
import com.skillstorm.reserveone.dto.OccupancyReportDTO;
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

    public int getCancellationsInPastWeek(UUID hotelId) {
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime weekAgo = now.minusDays(7);
        
        List<Reservation> cancellations;
        if (hotelId != null) {
            cancellations = reservationRepository.findByHotel_HotelIdAndStatusAndCancelledAtBetween(
                hotelId, Status.CANCELLED, weekAgo, now);
        } else {
            cancellations = reservationRepository.findByStatusAndCancelledAtBetween(
                Status.CANCELLED, weekAgo, now);
        }
        
        return cancellations.size();
    }

    public OccupancyReportDTO getOccupancyReport(UUID hotelId, LocalDate startDate, LocalDate endDate) {
        // Get total rooms
        List<Room> allRooms;
        if (hotelId != null) {
            allRooms = roomRepository.findByHotel_HotelId(hotelId);
        } else {
            allRooms = roomRepository.findAll();
        }
        
        int totalRooms = (int) allRooms.stream()
            .filter(room -> room.getStatus() != Room.Status.OUT_OF_SERVICE)
            .count();
        
        // Get all reservations in the date range
        List<Reservation> allReservations;
        if (hotelId != null) {
            allReservations = reservationRepository.findByHotel_HotelId(hotelId);
        } else {
            allReservations = reservationRepository.findAll();
        }
        
        // Filter reservations that overlap with the date range and are not cancelled
        List<Reservation> activeReservations = allReservations.stream()
            .filter(r -> r.getStatus() != Status.CANCELLED)
            .filter(r -> !r.getEndDate().isBefore(startDate) && !r.getStartDate().isAfter(endDate))
            .collect(Collectors.toList());
        
        // Initialize daily data map
        Map<LocalDate, DailyOccupancyDTO> dailyDataMap = new HashMap<>();
        LocalDate currentDate = startDate;
        while (!currentDate.isAfter(endDate)) {
            dailyDataMap.put(currentDate, new DailyOccupancyDTO(
                currentDate, 0, totalRooms, 0.0, 0, 0));
            currentDate = currentDate.plusDays(1);
        }
        
        // Process reservations to calculate occupancy
        for (Reservation reservation : activeReservations) {
            LocalDate resStart = reservation.getStartDate();
            LocalDate resEnd = reservation.getEndDate();
            
            // Count check-ins and check-outs
            if (!resStart.isBefore(startDate) && !resStart.isAfter(endDate)) {
                DailyOccupancyDTO dayData = dailyDataMap.get(resStart);
                if (dayData != null) {
                    dailyDataMap.put(resStart, new DailyOccupancyDTO(
                        dayData.date(),
                        dayData.occupiedRooms(),
                        dayData.totalRooms(),
                        dayData.occupancyRate(),
                        dayData.checkIns() + 1,
                        dayData.checkOuts()));
                }
            }
            
            if (!resEnd.isBefore(startDate) && !resEnd.isAfter(endDate)) {
                DailyOccupancyDTO dayData = dailyDataMap.get(resEnd);
                if (dayData != null) {
                    dailyDataMap.put(resEnd, new DailyOccupancyDTO(
                        dayData.date(),
                        dayData.occupiedRooms(),
                        dayData.totalRooms(),
                        dayData.occupancyRate(),
                        dayData.checkIns(),
                        dayData.checkOuts() + 1));
                }
            }
            
            // Count occupied rooms for each day of the stay
            LocalDate day = resStart.isBefore(startDate) ? startDate : resStart;
            LocalDate lastDay = resEnd.isAfter(endDate) ? endDate : resEnd;
            
            while (!day.isAfter(lastDay)) {
                DailyOccupancyDTO dayData = dailyDataMap.get(day);
                if (dayData != null) {
                    dailyDataMap.put(day, new DailyOccupancyDTO(
                        dayData.date(),
                        dayData.occupiedRooms() + 1,
                        dayData.totalRooms(),
                        dayData.occupancyRate(),
                        dayData.checkIns(),
                        dayData.checkOuts()));
                }
                day = day.plusDays(1);
            }
        }
        
        // Calculate occupancy rates and create final list
        List<DailyOccupancyDTO> dailyData = new ArrayList<>();
        double totalOccupancyRate = 0.0;
        double peakOccupancyRate = 0.0;
        LocalDate peakDate = startDate;
        int totalCheckIns = 0;
        int totalCheckOuts = 0;
        
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            DailyOccupancyDTO dayData = dailyDataMap.get(date);
            if (dayData != null) {
                double occupancyRate = totalRooms > 0 
                    ? (dayData.occupiedRooms() * 100.0 / totalRooms) 
                    : 0.0;
                
                DailyOccupancyDTO finalData = new DailyOccupancyDTO(
                    dayData.date(),
                    dayData.occupiedRooms(),
                    dayData.totalRooms(),
                    Math.round(occupancyRate * 100.0) / 100.0,
                    dayData.checkIns(),
                    dayData.checkOuts());
                
                dailyData.add(finalData);
                totalOccupancyRate += occupancyRate;
                totalCheckIns += dayData.checkIns();
                totalCheckOuts += dayData.checkOuts();
                
                if (occupancyRate > peakOccupancyRate) {
                    peakOccupancyRate = occupancyRate;
                    peakDate = date;
                }
            }
        }
        
        double averageOccupancyRate = dailyData.isEmpty() 
            ? 0.0 
            : Math.round((totalOccupancyRate / dailyData.size()) * 100.0) / 100.0;
        
        return new OccupancyReportDTO(
            dailyData,
            averageOccupancyRate,
            Math.round(peakOccupancyRate * 100.0) / 100.0,
            peakDate,
            totalCheckIns,
            totalCheckOuts);
    }
}
